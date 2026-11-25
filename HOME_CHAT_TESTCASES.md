# Home (unit chats) and Call Sheet unit: test cases

Suites: `tests/homechat/*` (HomeChatTest, ChatOptionsTest, PostingRightsTest, MediaPreviewTest,
MediaSystemPickerTest, ManualPickSendTest, AllUnitsMediaTest, TwoUserChatTest, RegressionStressTest,
**HomeWebGapTest**) and `tests/callsheet/CallSheetTest`, `CallSheetUploadModeTest`.
Page object: `pages/homechat/HomeChatPage`.

App source used for ids and rules: `Zillit-Android-devbuild` (build Zillit_32539-dev):
`bottomNav/home/Home.kt`, `baseUtils/CommonKeyboardFragment.kt`, `res/layout/fragment_home.xml`,
`emoji_keyboard_layout.xml`, `chat_text_layout.xml`.

## Web gap cases (added 2026-11-23)

Source: `web-e2e/gaps/home.md` (M1–M39) and `web-e2e/gaps/call-sheet.md` (CS-M1).
Every message is `ZL_AUTO`-prefixed and deleted in the case's `finally`; HomeWebGapTest also sweeps
its own leftover `ZL_AUTO-m…` rows in `@AfterAll`.

Markers carry a 7-letter stamp, never 7+ digits: `tvMessage` is `autoLink="all"`, and a digit run is
linkified as a phone number.

Results are from the last run on each device (see "Last result"). FAIL (bug) means the case
reproduces an open app bug and fails on purpose, naming it.

### HomeWebGapTest (`@Order(TestOrder.HOME_WEB_GAP)` = 205), one device

Runs: "Pixel" = 2026-11-23 run 4 on the Pixel; "emu" = 2026-11-24 full run on emulator-5560;
"emu rerun" = single-method reruns (`--only 'HomeWebGapTest.<method>'`) after a test fix.
Image, video, permission and offline cases run only on an emulator.

| Order | Web id | Case | Last result |
|---|---|---|---|
| 1 | M22 #25 #27 #28 | Unit strip labels real; Bulletin/Calendar/Call Sheet present; Calendar hides the composer | PASS (Pixel, emu) |
| 2 | M1 #33 | A pending reply quote is dropped when switching units | **FAIL (bug)**: the quote survives the switch — `Home.handleUnitChangeObserver` never calls `keyBoardFragment.resetUi()` (Pixel, emu) |
| 3 | M2 #41 #42 #57 (BUL-27) | Drafts per unit across unit switches and Home → Email → Home; a sent draft is cleared | PASS (Pixel, emu) |
| 4 | M3 #148 (BUL-28) | A post still sending when the unit changes ends as one sent row | PASS (Pixel, emu) |
| 5 | M6 #60 #146 (BUL-22) | A fast double tap on Send posts once and starts no recording | **FAIL (bug BUL-22)**: the second tap starts a voice recording (Pixel, emu) |
| 7 | M10 #77 #78 (BUL-12), #76 | Edit dialog: prefilled; Cancel keeps; unchanged Update adds no "Edited"; empty and >2,000 refused; real edit marks "Edited" | PASS (Pixel, emu) |
| 8 | M18 #87 #88, #84 #86 | Replying: quote names the sender; paperclip and mic hidden; spaces refused; reply nests once | PASS (Pixel, emu) |
| 9 | M23 #43 | Mic on Bulletin, none on Call Sheet; History only on Call Sheet | PASS (Pixel, emu) |
| 10 | M11 #20 #104 #105 #143 (BUL-14) | In-chat search: blank refused; exact match = 1; edited query searched again; own name matches; close restores | PASS (Pixel, emu rerun). One emu run returned 0 for the exact match before the search fallback was added; not reproduced since |
| 11 | M12 #4 #22 #108 #145 | Opens on the newest row; jump arrow only when scrolled away, brings the newest back | PASS (emu) |
| 12 | M14 #50–#55 | Markup literal; accents/symbols/RTL/emoji round-trip; >1,000 chars collapse behind "Read more" | PASS (Pixel, emu) |
| 13 | M15 #71 #72 #74 #19 | Own text menu: Reply/Edit/Delete; no Download, Image Reply, Copy, Distribute; back closes | PASS (Pixel, emu) |
| 14 | M16 #75 #81 #82 #118 #141 (BUL-09) | Selection mode: counts, deselect, back keeps, No keeps, unit switch exits, Yes deletes two | PASS (Pixel, emu) |
| 15 | M17 #85 #89 #90 #91 | Comment menu Edit/Delete/Read By; edit shows; No keeps; Yes removes | PASS (Pixel, emu) |
| 16 | M19 #93 #94 #96 (BUL-20) | Forward: Home and Members listed; Home unit list has no Calendar / no Call Sheet for a text | **FAIL (bug BUL-20)**: Call Sheet is offered (Pixel, emu) |
| 17 | M20 #98 (BUL-18) | Read By search: no match shows "No Users Found"; clearing hides it | PASS (Pixel, emu) |
| 18 | M21 #21 #106 #135 | Library tabs Media / Docs / Links (no Audio); a posted URL is counted under Links | PASS (emu rerun: `[Media (3), Docs (1), Links (2)]`) |
| 19 | M38 #10 #56 | Emoji picker opens, appends, closes on text-box tap; message posts with it | PASS (Pixel, emu) |
| 20 | M39 #9 #107 | `@a` opens no mention list; header shows sender and "MMM dd, yyyy hh:mm a" | PASS (Pixel, emu) |
| 21 | M30 #58 #83, M22 #31 | Post survives a relaunch; deleted post stays gone; one unit selected after relaunch | PASS (emu) |
| 22 | M24 #12 #62–#64 (BUL-24) | .docx and .xlsx post (Docs +2); .csv is pickable and posts | **FAIL (bug BUL-24)**: docx/xlsx post; `zl_drive_sheet.csv` cannot be picked (emu) |
| 23 | M27 #67, M39 #14 | Backing out of the image preview posts nothing; two images post two items | PASS (emu) |
| 24 | M26 #65, #66 | Edit a media caption: caption shows under the image, dialog prefilled, new caption shows | PASS (emu rerun). The first emu run lost the caption: `MediaPreviewPage.setCaption` presses BACK (hideKeyboard) in the preview — see "shared-file note" |
| 25 | M28 #100 #101, M21 #135 (BUL-19), M39 #102, #61 | PDF bubble size has a unit; tapping opens a viewer; library Docs size has a unit; Download confirms | **FAIL**: bubble "File Size : 426 B" OK; #102 opens OK; **BUL-19** library shows `426  ·  NOV 24, 2026` (no unit); Download showed no "File Downloaded Successfully." toast within 30 s in 3 runs — needs a manual look (emu reruns) |
| 26 | M25 #17 (BUL-23) | A PDF with /OpenAction JavaScript is refused | **FAIL (bug BUL-23)**: posted (Docs 1 → 2) (emu) |
| 27 | M29 #150 (BUL-34) | A posted video opens in the player and plays | PASS (emu rerun, SDK sample clip): the player opens; the position counter was not readable, so "playback advances" is not proven |
| 28 | M36 #35 | Overflow arrow shows exactly when there are more than 3 units | PASS (emu; 3 units, arrow hidden) |
| 29 | M39 #149 (BUL-32) | Scroll to older rows + switch unit: no error | PASS (emu) |
| 31 | #159 | Voice note: timer advances; the bin discards without posting | PASS (emu) |
| 32 | BUL-15 (Android-only) | Media permissions denied → Document opens the picker or explains | **FAIL (bug BUL-15)**: no picker and no message (emu) |
| 33 | M31 #119 #124 | Translate banner and composer toggle agree; no Translate on own message | PASS (emu: both hidden, same language) |
| 40 | M4 #112 #113 | Offline send → Retry; online Retry sends once | PASS (emu rerun) |
| 41 | M4 #114 #147 (BUL-25) | Delete an unsent offline row; stays gone after relaunch | **FAIL (bug BUL-25)**: "Internet connection lost" shown and the row stays (emu rerun) |
| 42 | M3 #148 offline (BUL-28) | Offline post during a unit switch: one failed row, not stuck on the clock | PASS (emu) |
| 43 | M39 #115 #92 | Bulletin offline: no crash/endless loader, composer usable; offline reply adds no comment | PASS (emu rerun) |
| 44 | M35 #37 #38 #136 #40 | Cold start offline lists cached units | PASS (emu) |
| 99 | M7 #142 (BUL-13) | 1,500 characters typed at machine speed: app alive, all text, heap bounded | **FAIL (bug BUL-13)**: Pixel — `OutOfMemoryError` crash (512 MB heap); emu — all 1,500 chars arrive but the Java heap grew 182 MB (64 → 251 MB); injecting took 78 s |

The suite starts by deleting rows earlier runs left behind (`ZL_AUTO-m1-base-1453025` and
`ZL_AUTO-m6-dbl-1582984`: removed on 2026-11-24) and ends by sweeping its own `ZL_AUTO-m…` rows and
1.44 KB fixture images.

### TwoUserChatTest additions, two devices (A = emulator acts, B = Pixel "Vidya Pixel")

| Order | Web id | Case | Last result |
|---|---|---|---|
| 8 | M5 #109 (BUL-16) | Another member's message offers no Edit | **not run**: two attempts skipped at setup (A parked in Doc Distribution; then B stuck on the Pixel's app lock after a cold start). Setup fixed; FAIL expected (`showEdit` has no owner check) |
| 9 | M9 #122 #160 | Unread badge on the unit chip; "New Messages" divider; badge clears | not run (no emulator + Pixel pair; see run notes) |
| 10 | M34 #152 (BUL-29) | A live edit keeps the replies under the post | not run (no emulator + Pixel pair; see run notes) |
| 11 | M20 #126 #162 | Read By updates live when B opens the unit | not run (no emulator + Pixel pair; see run notes) |
| 12 | M11 #153 (BUL-30) | Searching B's name finds a post B only replied to | not run (FAIL expected) |
| 13 | M8 #151 #95 (BUL-26) | A forwarded copy forwarded to a member reaches their C&C chat | not run (no emulator + Pixel pair; see run notes) |
| 7 | (observation) | `otherUserMessageInteractions`: Delete on B's message — admin gets selection mode (left without deleting), non-admin is refused | not run |

Also changed: systemPort 8260/8270, `ReasonLogger`, digit-free markers, package from config, and
the setup never cold-starts B (the Pixel): a cold start there raises Zillit's app lock
(BiometricPrompt, device credential), which a test must not answer.

Run notes (2026-11-24): the two-device lane got an emulator + the Pixel only at 05:12. The first
case skipped because B (Pixel) was left on the app-lock prompt by the setup's unconditional
relaunch (fixed since); the lane was stopped so it would not run the other six against a locked
device. All seven are written, compile, and are ready to run:
`ZILLIT_SENDER_UDID=<emulator> ZILLIT_RECEIVER_UDID=34281FDH2005XC ./run-and-report.sh --only 'TwoUserChatTest.<method>'`.

### CallSheetTest additions (Call Sheet unit text messages, CS-M1)

| Order | Web id | Case | Last result |
|---|---|---|---|
| 1 | CS #1 (strengthened) | History button **and** the composer show | PASS (Pixel, emu) |
| 20 | CS-M1 #2 | A typed text posts on Call Sheet and the composer clears | PASS (Pixel, emu) |
| 21 | CS-M1 #3 | Text options: Reply/Edit/Delete; no Forward, Share or Publish to Doc Distribution | PASS (Pixel, emu) |
| 22 | CS-M1 #4 | Edit: new text shows, marked "Edited" | PASS (Pixel, emu) |
| 23 | CS-M1 #5 | Reply: bar quotes the row; paperclip and mic hidden; reply nests under it | PASS (Pixel, emu) |
| 24 | CS-M1 #7 | Four emoji post | PASS (Pixel, emu) |
| 25 | CS-M1 #6 | Delete → Yes: the row is really gone | PASS (Pixel, emu) |
| 2, 3, 8 | CS #8 #6 (document) | Send / options / delete a document | PASS (emu). SKIP on the Pixel (no zl_test PDF there) |

### Weak covered cases strengthened

| Web # | Where | Change |
|---|---|---|
| #13 | HomeChatTest#eachMediaTypeOpens | backing out of every picker posts nothing (newest text + media count unchanged) — PASS (emu rerun) |
| #25 | HomeChatTest#unitsVisible | asserts Bulletin, Calendar and Call Sheet by name — PASS (emu) |
| #59 | — | not automatable: the pending clock clears in under a second online; the failed state is covered offline (M4a) |
| #61 | HomeWebGapTest M28 | PDF bubble shows its size with a unit — PASS ("File Size : 426 B") |
| #66 | HomeWebGapTest M26 | the caption shows under the image — PASS (emu rerun) |
| #71 | HomeWebGapTest M15 | absence asserts |
| #76 | HomeWebGapTest M10 | prefill and the "Edited" tag (PASS) |
| #84 #86 | HomeWebGapTest M18 | nesting in `rvMessageReply`, sender name in the reply bar (PASS) |
| #97 | ChatOptionsTest#shareOption | requires the action-bar Share and then the system share sheet — PASS (emu) |
| #159 | HomeWebGapTest order 31 | timer runs, the bin discards — PASS (emu) |
| (obs.) | ChatOptionsTest#readByOption / #forwardOption | require ReadByUserPage / ForwardPage, not "input bar gone" — PASS (emu) |

`AllUnitsMediaTest`'s comment saying Call Sheet is "document-only (no text)" was corrected.

### Not automatable (with reason)

- **M13** older messages on scroll-up: Bulletin history comes from the local Realm cache
  (`HomeVm.getChatListFromApi` → `homeChatDb` timestamps), so a scroll-up cannot tell an API page load
  from a cache read without a proxy, and the test units are kept short by per-suite cleanup.
- **M32** 30-minute edit/delete window: needs a non-admin poster; both test accounts are project
  admins, and changing a role is a project-settings change. (Wording to flag: "You can't delete message
  after 2 Hours" although the window is 30 minutes.)
- **M33** posting rights changed live, and the **C2** addition (view-only unit: Forward/Share/Edit give
  the posting-rights snackbar): both need a real member's rights set to view-only on the project that
  several suites use at the same time; while it is set, every other agent's B-device posting fails.
- **M37** Create Home unit offers departments: lives in `CreateUpdateHomeUnitTest` (admin-settings owner)
  and creates a unit on the shared project.
- **M31** French half and the two-device translation: a per-app locale switch turns every English text
  locator in the suite into French; the translated text itself is backend (BUL-11).
- **M12** long-row half (BUL-21): Android collapses text over 1,000 characters behind "Read more", so a
  long newest row is short and "lands at the top of the row" cannot happen as on web.
- **M39 #130** BMP from the gallery: the in-app gallery has no search, so a pushed BMP cannot be picked
  deterministically.
- **M39 #157** personal project: neither test account has a personal project.
- **M39 #79** edit/delete refused by the server: not stubbable on a device.
- **CS-03** comment badge for the other user: belongs to the Call Sheet Creation suite (CSC-40/43 send
  for comments; CSC-60 holds the badge locator).
- **C1** (2,000-character limit) and **C2** (Reply hidden for view-only): kept as Android behaviour.
