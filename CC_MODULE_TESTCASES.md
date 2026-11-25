# C&C (Chat & Calling) — Android test cases

Suites: `src/test/java/com/zillit/automation/tests/cc/` (CcGroupCreateTest, CcGroupChatTest,
CcGroupManageTest, CcDirectChatTest, CcChatMediaTest, CcContactsTest, CcCallTabTest, CcCallTest).
Page objects: `pages/cc/`.

Test data rules for C&C:
- Groups created by these cases are named `ZL AUTO …` (or `QA Cc Grp …` for the shared test
  group) and have **only the other test account** (`Vidya Pixel`, override with `ZILLIT_CC_PEER`)
  as a member. Each case deletes what it created in `finally`.
- 1-1 media cases run in the chat with `Vidya Pixel`, never with a real project member.
- No case places a call.

## Web gap cases (web E2E report vs Android, 2026-11-23)

Source: `web-e2e/gaps/candc.md`. "CC-n" is the row number in that file; CNC-01..03 are the sheet bugs.

| Web id | Android case | What it checks | Last result |
|---|---|---|---|
| CC-2 | `CcGroupCreateTest.chipsShowTheirCardTypes` | Members chip lists no group cards; every Groups card reads "N members in this group"; group cards carry no favourite star; the two lists differ | PASS (2026-11-24, emulator) |
| CC-3 | `CcGroupCreateTest.landingSearchFilters` | Groups chip: typing 4 letters keeps only matching names; a no-match query empties the list; clearing brings rows back. All chip: the box is the new-chat launcher (opens member search) — Android design, the web filters inline | PASS (2026-11-24) |
| CC-13 | `CcChatMediaTest.mediaDocsLinksOpens` | Thread header button opens "Media, Docs, Links" with tabs Media / Docs / Links; after sending an image Media reads "Media (n)"; each tab lists items when its label has a count, else shows the empty view; Back returns to the thread | PASS (2026-11-24): MEDIA (4), DOCS (4), LINKS (empty view); title "MEDIA, DOCS, LINKS" (rendered in caps) |
| CC-15 | `CcGroupChatTest.groupMessageArrivesLive` (two devices) | A creates a group with B; A's message appears on B live, B's reply appears on A live (20 s each). Needs `ZILLIT_RECEIVER_UDID`. Also sweeps `ZL AUTO ren …` groups left by CC-27 from B's side | NOT RUN yet (2026-11-24): implemented and compiled; the Pixel was held all night for its app-lock prompt. Run: `ZILLIT_UDID=<emulator> ZILLIT_RECEIVER_UDID=34281FDH2005XC ./run-and-report.sh --only CcGroupChatTest.groupMessageArrivesLive` |
| CC-18 | `CcChatMediaTest.sendTwoImagesAtOnce` | Two photos picked together → two image messages after a marker text, uploads finish | PASS (2026-11-24): 2 picked → 2 image bubbles after the marker, uploads finished |
| CC-20 | `CcChatMediaTest.sendAudioFile` | Attachment → Audio → system picker → `zl_test_audio.wav` → an audio message after a marker text, upload finishes | PASS (2026-11-24): zl_cc_audio.wav (pushed to Downloads by the test) → 1 audio bubble after the marker, upload finished |
| CC-22 | `CcContactsTest.toggleFavourite` (rewritten) | Star on Contacts moves the contact into the Favourites chip (+1 row, by name) and back out (restored); an empty Favourites chip shows "No favourites saved…" | PASS (2026-11-24) |
| CC-27 | `CcGroupManageTest.renamePromoteLeave` | New group with the peer → Edit Group rename shows in the thread toolbar → "Make group admin" gives 2 admin badges → Leave (shown with 2 admins) + confirm → the group is gone from my Groups list | PASS (2026-11-24): renamed shown in toolbar, admins 1 → 2, Leave + confirm, group gone from my list (it remains with Vidya Pixel as admin until the CC-15 sweep) |
| CC-28 | `CcGroupManageTest.addMember` (fixed) | Was `after >= 1` (never failed). Now: the peer is added via Edit Group, is listed by name, and the count rises by exactly 1 (the peer is removed first when already a member) | PASS (2026-11-24): 1 → 2, "Vidya Pixel" listed |
| CC-28 | `CcGroupManageTest.removeMember` (fixed) | Was `after < before \|\| after >= 1` (never failed). Now: the peer is removed, no longer listed, count drops by exactly 1 | PASS (2026-11-24): 2 → 1, "Vidya Pixel" gone |
| CC-29, CC-30, CNC-01 | `CcGroupCreateTest.groupsChipWithNoGroups` | Empty Groups chip: no loader after 10 s, chips tappable, Create Group stays, and an empty-state message is shown. Expected to FAIL on Android (ChatFragment shows `emptyView` only on Favourites) | SKIP: the suite enters the first project, which has C&C groups; needs a project with none |
| CC-31, CNC-02 | `CcGroupCreateTest.listHeightAfterSearchOpen` | Groups chip search (keyboard up) → open a group → Back → clear: the list bottom is within 8 px of before, and no keyboard-sized blank under the rows | PASS (2026-11-24): bottom 2190 → 2190, no CNC-02 symptom on Android |
| CC-32, CNC-02 | `CcContactsTest.listHeightAfterSearchOpen` | Same on Contacts (`recMembers`, chat opened via the row's chat icon) | PASS (2026-11-24): bottom 2190 → 2190, no CNC-02 symptom on Android |
| CC-33, CNC-03 | `CcGroupCreateTest.groupNameOverLimit` | 45 characters typed → field keeps exactly 40 (Android limit is 40, not 25) and something tells the user about the limit. Expected to FAIL: cut silently, no counter / hint / error | FAIL, real bug CNC-03 (2026-11-24): 45 typed → 40 kept, no hint/counter/error |
| CC-33 | `CcGroupCreateTest.groupNameBoundaries` | Names of exactly 40 and exactly 3 characters both save and are listed (deleted afterwards) | PASS (2026-11-24): "ZL AUTO len40 …" and "ZLa" saved, listed, deleted |
| CC-34, CNC-03 | `CcGroupManageTest.renameNameOverLimit` | Same 40-character cap and missing hint on the rename path (Edit Group), left without saving | FAIL, real bug CNC-03 (2026-11-24): rename field keeps 40 of 45, no hint/counter/error |

### Web-only (no Android equivalent)

| Web id | Why |
|---|---|
| CC-5 refresh chats | `fragment_chat.xml` has no refresh button or pull-to-refresh; the list updates over the socket |
| CC-16 theme toggle | C&C has no theme control; dark mode follows the app/system theme (`ChatAndCall.kt:123-129`) |
| CC-25 forward from media panel | `ChatLibraryActivity` has only Media/Docs/Links tabs and a click-to-preview handler; Forward lives on the message long-press (`CcGroupChatTest.forwardMessage`) |
| CC-26 share from media panel | Same; Share is on the message long-press (`CcGroupChatTest.shareMessage`) |

### Already covered before the web comparison

CC-1 `landingLoads`, `filterChips`; CC-4 `callTabLoads`, `contactsLoad`; CC-6/7/24 `directSendText`,
`directCopy`, `directEditMessage`; CC-8..11 `sendText`, `editMessage`, `replyToMessage`,
`deleteMessage`; CC-12 `inThreadSearch`, `groupMediaParity`; CC-14 `groupInfoLoads`;
CC-17/19/21 `sendImageFromGallery`, `sendDocument`, `sendLocation`; CC-23 `searchContacts`.

### Notes
- Android landing search differs from web: on the All and Members chips the search box is covered
  by `layHideSearch`, which opens SearchUsersPage (start a new chat); the inline filter works on
  Groups / Unread / Favourites (MembersVM `searchOrSubmitUserGroupList`).
- Office documents in C&C opening in another app is covered by `ChatDocumentEditorTest`, not here.
- `memberOptions` and the shared `QA Cc Grp` group now prefer the peer test account; the old
  "first two people in the picker" is only a fallback when the peer is not listed.
