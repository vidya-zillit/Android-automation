# Catering — test cases and web-gap coverage

Suites: `CateringTest` (single device, extends `ToolChatBaseTest`) and `CateringTwoUserTest`
(crew device + caterer device). Page object: `pages/tools/CateringPage.java`.
Web gap analysis: `side-session-notes/.../web-e2e/gaps/catering.md` (85 missing web cases, M1–M33).

## Roles (since 2026-11-23)

| Device | User | Catering role |
|---|---|---|
| emulator-5554 / emulator-5556 | Vidya Testing | crew member (orders / posts to the caterer) |
| Pixel 7 `34281FDH2005XC` | Vidya Pixel | caterer (Catering department) |

Rules read from the app source (`CateringActivity.kt`, `CateringVm.kt`, `TextVH.kt`):
- posting is blocked for everyone while no enabled member is in the catering department
  (`canSendMessage()`, snackbar "You cannot post any message until a person joins the catering department");
- a crew post is labelled "To : All Caterer"; a caterer posts to "All Users" or to one picked user;
- the caterer has no composer until a recipient is chosen (`addMember` / `layNoMsg`);
- Reply is hidden on caterer-authored messages; the caterer may reply to crew posts;
- Catering never offers Read By, Copy, Print or Image Reply; Edit only on own messages (media: only with a caption);
- delete selection: an admin may select any row; others get "You have no permission to delete other user message."
  (or "You can't delete message after 2 Hours").

## Today's Pixel failures (re-checked on an emulator)

The 4 failures in the Pixel run of 18:12 (Forward, Gallery, "message survives a meal-tab switch",
RULE 2) were **not product bugs**: at that time nobody was in the catering department, so every send was
refused ("RULE 1 — no caterer" passed in the same run with the warning shown). Messages only echoed locally,
so they vanished on reload and never carried a "To : All Caterer" label. On the emulator (crew, caterer now
assigned) all four pass: Forward, Gallery, persistence across a meal-tab switch and RULE 2 (crew6 and crew9).

## Web-gap cases added

Results: "crew9" = emulator-5554, Vidya Testing (crew), 2026-11-24 05:50–06:52. Cases marked
*fixed, rerun pending* failed for a test reason that has since been corrected (see notes).

`CateringTest` — crew-side and both-roles cases:

| Order | Web id | Case | Last result |
|---|---|---|---|
| 6 | CAT-59 (C2) | Picker is exactly Photo / Image Gallery / Audio / Document / Multiple Capture | PASS |
| 12 | CAT-594 (C1) | Read By is never offered | PASS |
| 60 | CAT-194 (M3) | Breakfast draft survives a Lunch visit | **FAIL — app bug CAT-02** (draft lost) |
| 61 | CAT-282 (M3) | Draft survives leaving and reopening Catering | **FAIL — app bug CAT-02** |
| 62 | CAT-352 (M10) | Own text menu is the Catering set (no Read By/Copy/Print/Download) | PASS |
| 63 | CAT-542 (M10) | Another member's message offers no Edit | PASS |
| 64 | CAT-357 (M11) | Own document without caption: Download, no Edit | fixed, rerun pending (PDF picker) |
| 65 | CAT-362 (M11) | Own captioned image: Download + Edit, no Edit and Post / Image Reply | fixed, rerun pending (gallery album) |
| 66 | CAT-367 (M11) | Own audio: Download, no Edit | fixed, rerun pending (audio fixture into Download) |
| 67 | CAT-527 (M31) | Gallery → Docs lists the document with a readable size (CAT-05) | pending (needs case 64) |
| 68 | CAT-215 (M33) | No REQUEST bar; paperclip present | PASS |
| 69 | CAT-724/742 (M24) | Delete selection refuses another member's row | refusal PASS; final scroll check fixed, rerun pending |
| 80 | CAT-557 (M1) | Offline send shows Retry, Retry offline refused, online delivers once | **FAIL — app bug**: the offline message never appears and gets no Retry (`CateringVm.sendTextMessage` waits for `translateTextToOtherLanguage`, a network call, before saving the row) |
| 81 | CAT-44 (M2) | 2,000 chars post; 2,001 refused (CAT-04) | PASS |
| 82 | CAT-387 (M9) | Empty edit refused, dialog stays open | PASS |
| 83 | CAT-387 (M9) | Unchanged Update not "Edited" (CAT-03) | PASS (CAT-03 fixed on this build, BUL-12) |
| 84 | CAT-104/517 (M4) | Search finds a marker, empty refused, nonsense counts 0 | PASS |
| 85 | CAT-522/104 (M4) | Own-name search; space-only refused | **FAIL — app gap**: a space-only query matches every row (count 37) instead of being refused (crew6) |
| 86 | CAT-307/312/317 (M5) | XLSX / DOCX / CSV upload with caption | XLSX PASS; DOCX/CSV split into own cases, rerun pending |
| 87 | CAT-332 (M6) | Two images at once → two rows | fixed, rerun pending |
| 88 | CAT-74 (M7) | Backing out of the preview posts nothing | PASS |
| 89 | CAT-322 (M8) | Captioned document Edit → new caption + "Edited" | PASS |
| 90 | CAT-382 (M9) | Edit → Cancel keeps the text, not "Edited" | PASS |
| 91 | CAT-387 (M9) | Edit over 2,000 characters refused | fixed, rerun pending (snackbar is behind the dialog; asserted by effect) |
| 92 | CAT-497/502 (M12) | Download on document and image → "File Downloaded Successfully." | pending (needs case 64) |
| 93 | CAT-512/507 (M13) | Image viewer opens/closes; PDF hands off | fixed, rerun pending (viewer is CustomImageViewer) |
| 94 | CAT-372 (M14) | Selection mode counts, hides composer, Back leaves | PASS |
| 95 | CAT-402 (M15) | Delete backed out at selection and at No | PASS |
| 96 | CAT-407/412 (M15) | Two selected deleted together, gone after reopen | PASS |
| 97 | CAT-427/432 (M16) | Reply bar quotes, hides paperclip, dismiss restores | PASS |
| 98 | CAT-437 (M16) | Whitespace-only reply refused, quote kept | PASS |
| 100 | CAT-422/442/447 (M17) | Comment renders once; menu Edit/Delete; edit; No keeps; Yes deletes | fixed, rerun pending (arrow tap) |
| 101 | CAT-54/277 (M18) | Emoji picker appends at the end, sends, closes | PASS |
| 102 | CAT-247 (M19) | Read more / Read less | collapse PASS; expand tap fixed, rerun pending |
| 103 | CAT-257/262 (M20) | https link and e-mail open outside the app | PASS |
| 104 | CAT-252/272 (M21) | HTML literal; special characters round-trip | PASS |
| 105 | CAT-114/24/537 (M22) | Opens on newest; arrow on scroll; arrow returns | PASS |
| 106 | CAT-119 (M22) | Older page loads | skips when the meal has < 50 rows (Breakfast had 38) |
| 107 | CAT-49/99 (M33) | No mention picker; menu closes on Back | rerun pending (UiAutomator timeout chain) |
| 108 | CAT-297 (M33) | Double tap on Send posts once | rerun pending (UiAutomator timeout) |
| 109 | CAT-462/467 (M33) | Forward destinations; Home list without Calendar/Call Sheet | rerun pending |
| 110 | CAT-532 (M33) | Row shows my name and the time format | rerun pending |
| 111 | CAT-287 | Message persists across reopen | rerun pending |
| 112 | CAT-377 | Edit marks "Edited" | rerun pending |
| 113 | CAT-205/210 | Crew has no "+" and no recipient chip | PASS |
| 20–33 | CAT-131/167/172/589 | Meal tabs (no raw label keys), isolation, persistence, RULE 1/2/3b | PASS |

Caterer-side cases in `CateringTest` (run on the Pixel; skip with "Not a caterer" on crew devices):
40–53 (recipient gating, dialog, RULE 4/5, directed label "To : <user>", "+" form, empty name, create /
rename / delete unit), 51 M27 duplicate name, 70 M23 receiver leak, 71 M26 Catering Settings, 114 Select
Users modal — **pending the Pixel run** (the first Pixel run was void: the Tools list re-sorted under the tap
and opened Confidential Info; Catering is now opened by tile text and verified by its meal strip).

`CateringTwoUserTest` (A = crew emulator, B = caterer Pixel) — **pending**:

| Order | Web id | Case |
|---|---|---|
| 1 | CAT-677 (M28) | Caterer's new / edited / deleted message reach the crew member live |
| 2 | CAT-632/683 (M28) | Crew post reaches the caterer live ("To : All Caterer"); caterer's reply reaches crew live |
| 3 | CAT-626/646/665 | Crew sees the broadcast as "To : All Users" with no Reply / Edit |
| 4 | CAT-622 (M28) | A directed caterer message reaches the picked user |
| 5 | CAT-689 (M29) | Lunch badge on the crew tab; "New Messages"; badge clears |
| 6 | CAT-628/724 (M24) | Crew cannot add the caterer's message to a delete selection |
| 7 | CAT-706 (M28) | A unit the caterer creates / deletes follows on the crew's tabs live |
| 8 | CAT-695 (M29) | Caterer post raises the crew's Catering tile badge |
| 9 | CAT-701 (M30) | Notification for a Lunch post opens Catering on Lunch |
| 10 | CAT-726/748/634 (M25) | Without the Download right, Download offers the admin request (right restored) |
| 11 | CAT-728 (M25) | Without the View right, Tools has no Catering (right restored) |

## Not automatable (with reason)

- M1 server-refusal variants (CAT-562, CAT-616 — POST answered with status 0/1) and all of M32 (CAT-177,
  183, 392, 457, 572 — failing unit list / edit / comment / page GET): need a stubbed backend or proxy,
  which the Appium setup does not have.
- M33 "pending row shows the clock" (CAT-292): the clock (status 0) is visible only while a POST is in
  flight; offline goes straight to the failed state, and a throttled network is not available.
- Offline cases run on emulators only (toggling Wi-Fi/data on the shared Pixel would cut it for others).
- Role/right-changing cases (M25 in CateringTwoUserTest) run only while the run holds both devices' locks
  (`ZILLIT_ROLE_CHANGES_OK=1`); they restore the right in `finally`.
