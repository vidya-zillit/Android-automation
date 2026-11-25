# C&C: web cases vs Android suite

Web input: `candc.md` (34 web tests: 28 regular and 6 sheet-bug specs; plus 3 sheet bugs CNC-01..03).
Android suites: `tests/cc/` (CcGroupCreateTest, CcGroupChatTest, CcGroupManageTest, CcDirectChatTest, CcChatMediaTest, CcContactsTest, CcCallTabTest, CcCallTest).
App source:
- `chatAndCall/` stands for `Zillit-Android/app/src/main/java/com/zillit/zillitapp/bottomNav/chatAndCall/`.
- `chatAndGroupChat/` stands for `.../zillitapp/chatAndGroupChat/`.

## Summary

| # | Web test / sheet bug | Status | Android case / note |
|---|---|---|---|
| 1 | chat list loads with tabs, filters, empty prompt | COVERED (partial) | `CcGroupCreateTest.landingLoads`, `filterChips`. The "Click on any User or group…" prompt is web-only (split pane) |
| 2 | Members and Groups filters show their card types | **MISSING** | `filterChips` taps Groups and only checks that the list is still usable |
| 3 | search filters the chat list | **MISSING** | No test types into the landing `searchEditTextVw` |
| 4 | Calls and Contacts tabs render | COVERED | `CcCallTabTest.callTabLoads`, `CcContactsTest.contactsLoad` |
| 5 | refresh chats re-renders the list | WEB-ONLY | No refresh control in `fragment_chat.xml` (ids at :11-175: no button, no SwipeRefreshLayout) |
| 6 | open a personal chat and send | COVERED | `CcDirectChatTest.directSendText` |
| 7 | copy a personal message | COVERED | `CcDirectChatTest.directCopy` |
| 8 | open a group chat and send | COVERED | `CcGroupChatTest.sendText` |
| 9 | edit an own group message | COVERED | `CcGroupChatTest.editMessage` |
| 10 | reply to an own group message | COVERED | `CcGroupChatTest.replyToMessage` |
| 11 | delete an own group message | COVERED | `CcGroupChatTest.deleteMessage` |
| 12 | search messages panel finds a sent message | COVERED | `CcChatMediaTest.inThreadSearch`, `groupMediaParity` |
| 13 | media panel opens from the chat header | **MISSING** | `mediaDocsLinksBtn` is never tapped |
| 14 | group header opens the members panel | COVERED | `CcGroupManageTest.groupInfoLoads` |
| 15 | realtime: a message sent in one session appears in another | **MISSING** | Needs two devices. The infrastructure exists (`homechat/TwoUserChatTest`) |
| 16 | theme toggle switches light/dark | WEB-ONLY | C&C has no theme button. The app follows the app theme (`chatAndCall/ChatAndCall.kt:123-129` only adapts colours) |
| 17 | upload a single image to a group | COVERED | `CcChatMediaTest.sendImageFromGallery` (on a 1-1 thread, same `ChatAndGroupPage`) |
| 18 | upload multiple images at once | **MISSING** | `sendImageViaGallery` always picks 1 |
| 19 | upload a document | COVERED | `CcChatMediaTest.sendDocument` |
| 20 | upload an audio file | **MISSING** | Only the presence of the "Audio" option is checked |
| 21 | send current location | COVERED | `CcChatMediaTest.sendLocation`. Best-effort; the location tool is deferred by design |
| 22 | mark a member as favourite, then unmark | **MISSING** | `CcContactsTest.toggleFavourite`'s assertion is always true, and the Favourites chip is never checked |
| 23 | search a user in the contacts list | COVERED | `CcContactsTest.searchContacts` |
| 24 | edit an own personal message | COVERED | `CcDirectChatTest.directEditMessage` |
| 25 | forward media from the media panel | WEB-ONLY | The Android library has no forward action (see below) |
| 26 | share media from the media panel opens email compose | WEB-ONLY | Same: the library has no share action |
| 27 | group admin flow: rename, promote, leave | **MISSING** | `memberOptions` only opens the menu. There is no rename, Make admin or Leave case |
| 28 | group lifecycle: create, add member, leave | COVERED (weak) | `createGroup`, `CcGroupManageTest.addMember`, `deleteGroup`. The `addMember` assertion is `after >= 1` |
| 29 | Groups filter clears loading when there are no groups (stub) | **MISSING** | Merged with CNC-01 |
| 30 | Groups filter shows an empty state when there are no groups (stub) | **MISSING** | The Android source has the same gap, so this would fail today |
| 31 | Chat list keeps full height after opening a chat from search | **MISSING** | Merged with CNC-02 |
| 32 | Contacts list keeps full height after opening a chat from its search | **MISSING** | Merged with CNC-02 |
| 33 | New Group name over 25 characters is kept, and the limit is shown | **MISSING** | The Android limit is **40**, not 25 |
| 34 | renaming a group past 25 characters | **MISSING** | Same field on Android (Edit Group reuses CreateGroupPage) |
| CNC-01 | Group tab spinner never clears with no groups | **MISSING** | — |
| CNC-02 | Contacts list keeps a keyboard-sized blank area | **MISSING** | — |
| CNC-03 | Group name cut at 25 with no message | **MISSING** | Android: cut at 40 with no message |

Counts (37 items): 16 COVERED, 17 MISSING, 4 WEB-ONLY, 0 CONFLICT.

---

## MISSING, in full

### 2: Members and Groups chips list their own row types

The Android chips come from `MembersVM` (chatAndCall/viewModels/MembersVM.kt:650-700): All, Unread, Members, Groups (hidden while the project is PENDING), Favourites and Department. A row is `chat_group_listing_rv_vw`, and for a group its `designationVw` reads "N members".

- **Proposed case:** `CcGroupCreateTest.chipsShowTheirCardTypes`
- **Steps:**
  1. Tap the `tavViewRv` chip "Members". Read the first 5 rows of `mainRvView` (`name`, `designationVw`).
  2. Tap the chip "Groups" and read the rows again.
- **Expected:**
  - Under Members, no `designationVw` matches `^\d+ members?$`.
  - Under Groups, every row's `designationVw` matches `^\d+ members?$`.
  - The row sets differ.
- **Skip if** Groups has 0 rows.

### 3: chat list search narrows to matching names

Search is visible only on the All and Members chips: `layHideSearch.isVisible = data == members || data == all` (chatAndCall/fragment/ChatFragment.kt:120). The filter runs on every keystroke through `searchOrSubmitUserGroupList()` (ChatFragment.kt:241-244).

- **Proposed case:** `CcGroupCreateTest.landingSearchFilters`
- **Steps:**
  1. Tap the chip "All".
  2. Take the first row `name` with 3 or more letters, and type its first 4 letters into `searchEditTextVw`.
- **Expected:** every listed `name` contains those letters (case-insensitive), with 1 or more rows.
- **Then:**
  1. Type `zzz-nomatch-<epoch>`. Expect 0 rows in `mainRvView`.
  2. Clear the search with `crossIcon`. The rows come back.
- **Also assert:** on the "Groups" chip, `searchVw` / `layHideSearch` is not displayed. This pins the Android design.

### 13: Media, Docs, Links panel opens from the thread header

The `mediaDocsLinksBtn` button (activity_chat_and_group_page.xml:168) calls `openChatLibrary(...)` (chatAndGroupChat/ChatAndGroupPage.kt:480-486). That opens `ChatLibraryActivity`, titled "Media, Docs, Links" (strings.xml:3899).

- **Proposed case:** `CcChatMediaTest.mediaDocsLinksOpens`
- **Steps:**
  1. Open the QA group (`openOrCreateTestGroup`).
  2. Send an image (`sendImageViaGallery`, best-effort).
  3. Tap `mediaDocsLinksBtn`.
- **Expected:**
  - The toolbar reads "Media, Docs, Links".
  - `tlTabs` has three tabs whose text starts with "Media", "Docs" and "Links". Each label gets " (n)" when it has items (ChatLibraryActivity.kt:218-226).
  - When an image was sent, the Media tab reads "Media (n)" with n ≥ 1, and `rvContent` has 1 or more children. Otherwise `emptyView` is shown.
  - Tapping the Docs and Links tabs swaps the list without a crash.
- **Then:** go back and expect `etMessage` again.

### 15: realtime delivery between two sessions (two devices)

- **Proposed case:** `CcTwoUserChatTest.groupMessageArrivesLive`, built like `tests/homechat/TwoUserChatTest.java`. That test needs `ZILLIT_RECEIVER_UDID`; device B is a different user. Android cannot run the same account twice.
- **Steps:**
  1. On A and B, open C&C → chip "Groups" → a group both are in (create it on A with B as a member).
  2. On A, send `[e2e] realtime <epoch>`.
- **Expected:**
  - On B the message appears in `rvVw` within about 15 s, with no back or reopen.
  - Repeat from B to A.
- Clean up the group on A (`deleteGroup` flow).

### 18: several images picked together arrive as several messages

- **Proposed case:** `CcChatMediaTest.sendTwoImagesAtOnce`
- **Steps:**
  1. Count `imageBubbleCount()`.
  2. Open the attachment sheet → "Gallery" → `MediaPreviewPage.openAllMediaAlbumIfListed()`.
  3. Call `selectPhotos(2)`, then `tapGallerySendFab()` → preview → `tapPreviewSend()`.
- **Expected:** `imageBubbleCount()` rises by 2 within 40 s.
- **Skip if** the gallery has fewer than 2 photos.
- **Note:** `CcChatThreadPage.sendImageViaGallery` hard-codes `selectPhotos(1)`, so add a parameter instead of copying the method.

### 20: audio file upload

The "Audio" attachment option starts the `PICKER_ITEM_PICK_AUDIO` picker (ChatAndGroupPage.kt:2381).

- **Proposed case:** `CcChatMediaTest.sendAudioFile`
- **Steps:**
  1. Push `zl_test.wav` (or `.mp3`) to `/sdcard/Music` once in setup.
  2. Count `cardBubbleCount()`.
  3. Open the attachment sheet → "Audio" → pick `zl_test` in the system picker, the same way as `sendDocument("zl_test")` → confirm.
- **Expected:** `cardBubbleCount()` rises by 1 within 40 s (an audio card, `mainCardVw`).
- **Skip** when the picker cannot be navigated.

### 22: favourite then unfavourite, checked on the Favourites chip

Current bug in our test: `CcContactsTest.toggleFavourite` asserts `after != before || after >= 0`, which is always true.

- **Proposed case:** rewrite `CcContactsTest.toggleFavourite` as follows.
- **Steps:**
  1. On the Chat tab, tap the chip "Favourites" (string `favorite` = "Favourites") and count the rows of `mainRvView` → `f0`. If `f0 == 0`, expect `emptyView` with the text starting "No favourites saved." (`no_fav_placeholder`; ChatFragment.kt:247-256).
  2. On the Contacts tab (`tlTab` "Contacts"), find the first row whose `isFevouritesIv` is not shown, and tap its `favorite_icon`. Remember the `userNameTv`.
  3. Back on Chat → "Favourites".
  4. Contacts → tap the same contact's `favorite_icon` again.
  5. Chat → "Favourites".
- **Expected:**
  - After step 3: `f0 + 1` rows, including that name.
  - After step 5: `f0` rows again.

### 27: group admin flow (rename, make admin, leave)

How Android does it:
- Rename is **Edit Group** (`editGroup` → CreateGroupPage with `gpName` prefilled). There is no inline name editor.
- "Make group admin" is a member long-press option (`make_group_admin`, strings.xml:817). It applies with **no confirmation** (ProfileShowingActivity.kt:951-955).
- `leaveButton` shows only when the group has more than 1 admin, or when you are not an admin (ProfileShowingActivity.kt:760-762). Its confirm text is "Are sure you want to leave this group?" (strings.xml:815, sic).

**Proposed case:** `CcGroupManageTest.renamePromoteLeave`

**Steps:**
1. Create a group `ZL AUTO adm <epoch>` with 1 other member (`createGroup` helper).
2. Open it → toolbar `userName` → group info.
3. Tap `editGroup`, set `gpName` to `ZL AUTO ren <epoch>`, then Submit.
4. Back in the thread, check that the toolbar `userName` shows the new name.
5. Open group info again. Long-press the other member (`membersRv`, index 1) → "Make group admin".
6. Check that 2 rows show `adminHeader`.
7. Check that `leaveButton` is displayed. Tap it, then confirm.
8. Go back to C&C → chip "Groups".

**Expected:**
- The new name shows after step 4.
- 2 admins after step 6.
- After leaving, no `mainRvView` row is named `ZL AUTO ren <epoch>`.

**Cleanup note:** the group survives with the other admin. Have device B or a later sweep delete `ZL AUTO ren*`, or accept the leftover.

### 29, 30, CNC-01: Groups chip when the project has no groups

The Android source has the same empty-state gap as web ZL-17468. The empty view is shown only on the Favourites chip: `binding.emptyView.emptyMainLi.isVisible = size == 0 && isFavTab` (ChatFragment.kt:247-256). With no groups, the Groups chip is a blank list.

Android cannot stub the network, so the case needs a project with no CNC groups. Use the personal project, or a dedicated empty project selected through `ZILLIT_PROJECT`, or run it right after `CcGroupManageTest.deleteGroup` when that was the only group.

- **Proposed case:** `CcEmptyGroupsTest.groupsChipWithNoGroups`
- **Steps:**
  1. Open C&C and tap the chip "Groups". Wait 10 s.
  2. Tap "All", then "Groups" again.
  3. Receive a message or reopen the tab, to trigger the refresh the sheet mentions.
  4. Tap "Members".
- **Expected:**
  - (a) No loader or progress view stays on screen longer than 10 s, and the chips are still tappable (CNC-01).
  - (b) `createGroupBtn` is still displayed on Groups.
  - (c) Under Members, rows are listed.
  - (d) **Known failure:** `emptyView` with a "no groups" message is expected but not shown. Mark the case `@Tag("known-bug")` until product adds an empty state.

### 31, 32, CNC-02: the list keeps its full height after opening a chat from search

- **Proposed case:** `CcGroupCreateTest.listHeightAfterSearchOpen` and `CcContactsTest.listHeightAfterSearchOpen`
- **Steps (Chat tab):**
  1. Chip "All". Read the `mainRvView` bounds (`getRect()`) → `h0`.
  2. Tap `searchEditTextVw` so the soft keyboard is **up**. Type the first 4 letters of a known member.
  3. Tap the first result. `ChatAndGroupPage` opens.
  4. Press back **while the keyboard is still up** (do not call hideKeyboard first). Clear the search with `crossIcon`.
  5. Read the `mainRvView` bounds again → `h1`.
- **Expected:**
  - The bottom of `h1` is within 8 px of `h0`'s bottom.
  - The last visible row ends at or below `h0.bottom - rowHeight`.
- **Contacts tab:** the same steps on `recMembers` with `search_widget_edit_textvw`, opening the chat through `message_button`.

### 33, 34, CNC-03: group name length limit

What Android does:
- The name field is the shared widget `edit_text_widget_no_margins`, which has `android:maxLength="40"` (edit_text_widget_no_margins.xml:36).
- On submit, length < 3 or > 40 shows "Please Enter Group Name of length at least 3 characters or at most 40 characters." (`group_name_validate`, strings.xml:1388; CreateGroupPage.kt:383-391).
- Because of maxLength, the over-40 branch can never fire. Typing stops at 40 with no hint, which is the same symptom as CNC-03, but at 40, not 25.
- Rename uses the same screen (`editGroup` → CreateGroupPage), so 33 and 34 are one case on Android.

**Proposed case:** `CcGroupCreateTest.groupNameOverLimit`

**Steps:**
1. Tap `createGroupBtn`.
2. In `gpName` → `editTextInput`, type a 45-character name: `ZL AUTO cnc-03 ` + 30 × `x`.
3. Read the text back.

**Expected (current behaviour, pinned):**
- The text is exactly 40 characters.
- No error or counter is shown under the field (`editTextWidget.error` is null).

**What to report:** this is the CNC-03 symptom on Android. Once product decides (allow longer names, or show the limit), change the assertion. A `counterEnabled` with `counterMaxLength=40`, or a hint, would satisfy "limit shown".

**Also:**
- Enter exactly 3 and exactly 40 characters and check that both submit. Use real members and delete the groups afterwards.
- Run the same steps from `CcGroupInfoPage.tapEditGroup()` for the rename path.

---

## WEB-ONLY

- **5: Refresh chats.** `fragment_chat.xml` has no refresh button or pull-to-refresh. The list updates over the socket.
- **16: Theme toggle.** C&C has no theme control on Android. Dark mode is the app or system theme (`chatAndCall/ChatAndCall.kt:123-129`).
- **25 and 26: Forward or Share from the media panel.** `ChatLibraryActivity` shows only the Media, Docs and Links tabs (ChatLibraryActivity.kt:81-83). Its adapter has a single click-to-preview handler (`chatAndGroupChat/library/ChatLibraryAdapter.kt:69-72`), with no forward, share or email-compose action. On Android, Forward and Share live on the message long-press menu, which `forwardMessage` and `shareMessage` cover.

---

## COVERED (compact)

- **1 (partial):** `landingLoads` asserts the search and `createGroupBtn`. `filterChips` asserts that "Groups" or "All" is present. Optional: assert the `tlTab` labels Chat, Call and Contacts, and the chips All, Unread, Members, Groups and Favourites.
- **4:** `callTabLoads`, `contactsLoad`.
- **6, 7, 24:** `directSendText`, `directCopy`, `directEditMessage`. Optional: assert `etMessage` is empty after send (web checks it).
- **8–11:** `sendText`, `editMessage`, `replyToMessage`, `deleteMessage`.
- **12:** `inThreadSearch`, `groupMediaParity`.
- **14:** `groupInfoLoads`.
- **17, 19, 21:** `sendImageFromGallery`, `sendDocument`, `sendLocation`. These run on a 1-1 thread; `groupMediaParity` covers the group sheet.
- **23:** `searchContacts`.
- **28 (weak):** `CcGroupManageTest.addMember` ends with `after >= 1` and the comment "toggles a member (add or remove)". Assert `after == before + 1` after choosing a member who is not already in the group. `removeMember` has the same weakness (`after < before || after >= 1`).
