# Schedule Full & One Line: sheet bugs vs Android suite

Web input: `schedule-full-and-one-line.md` (0 web tests, 4 sheet bugs).
Android suite: `tests/scheduledist/ScheduleDistTest.java` (SD-01..99). Doc: `SCHEDULE_DIST_MODULE_TESTCASES.md`.
App source: `Zillit-Android/.../bottomNav/tools/schedule/`, called `sched/` below.

## Summary

| Bug | Title | Status | Android case / note |
|---|---|---|---|
| SCH-01 | An undated One Line upload made right after a dated one shows the earlier date | **MISSING** | SD-13 and SD-20 upload sentinels but never compare an undated upload with a dated one. On Android the date line is always shown, with an empty value when undated |
| SCH-02 | Scene search is case-sensitive, and fast typing can end on "No Schedule Page found" | **MISSING** | SD-36 searches the exact scene once. No case-change or fast-typing case |
| SCH-03 | Tools keeps a red badge on Schedule Full & One Line after the upload was deleted | **MISSING** | Needs two devices (A uploads and deletes, B reads the badge) |
| SCH-04 | An empty scene row stays in Pages after its last page is deleted | **MISSING** | The doc's SD-44 expects "the folder leaves the Pages tab", but the code only checks that the page left the folder |

Counts: 4 MISSING, 0 COVERED, 0 WEB-ONLY, 0 CONFLICT.

---

## MISSING, in full

### SCH-01: an undated One Line does not inherit the previous upload's date

Facts from the source:
- The upload date is an instance field of `UploadDocumentActivity` (`epochDate`, forms_signature/activity/UploadDocumentActivity.kt:135). It is only seeded from `REVISION_DATE` on **Replace** (:277-278; sched/ScheduleOneLineFragment.kt:362, sched/views/ScheduleFragment.kt:373). A fresh upload should start with no date.
- The row **always** shows the label, even when there is no date: `"One Line date : " + (date if > 0 else "")` (sched/holder/ScheduleHolder.kt:26). So on Android the expectation is "the label is there with an **empty** value". The web expects "no Schedule Date row". This is a platform difference, not a conflict, because no current test asserts it.

**Proposed case:** `ScheduleDistTest.SD_25_undatedOneLineHasNoDate`

**Steps:**
1. In the same app session as SD-13 (a dated Full upload), go to the "Schedule One Line" tab → "Upload One Line".
2. Pick the PDF and confirm. Do **not** tap "Click to add date (Optional)". Submit.
3. Read the new top row's `tvName`.

**Expected:**
- `tvName` matches `^One Line date :\s*$`: label only, no date.
- In particular, it does not contain SD-13's date.

**Cost:** Full and One Line rows **cannot be deleted** (doc, "Full and One Line cannot be cleaned up from the UI"), so each run leaves a row behind. Two ways around that:
- Run it only on a throw-away project, behind an opt-in flag like SD-51.
- Or fold it into the sentinel itself: upload the One Line sentinel undated in SD-20. That run makes no extra rows, but the date line changes, and SD-20's `startsWith("One Line date :")` still holds.

### SCH-02: scene search ignores case and survives fast typing

Facts from the source:
- The search is server-side, sent from `SchedulePageFolderFragment` with `.debounce(700).onEach { performSearch(it) }` (sched/views/SchedulePageFolderFragment.kt:207-210).
- `onEach` does **not** cancel an earlier request, so an older response can land last. The race exists on Android too.
- Case-sensitivity is a server matter.

**Proposed case:** `ScheduleDistTest.SD_36b_sceneSearchCaseAndSpeed`

**Steps:**
1. Use the SD-35 fixture, whose scene number carries letters (`99ZLAUTO<run>…`).
2. On Pages, search `scene.toLowerCase()` and wait for `settledFolders()`.
3. Clear the search. Type `scene` character by character, with about 80 ms between keys (`sendKeys` one character at a time).
4. Wait 3 s, then read again.

**Expected:**
- (a) After step 2, a folder titled "Scene No. <scene>" is listed.
- (b) After step 4, the same folder is listed, and `emptyView` ("No Schedule Page found") is **not** shown.
- Repeat (b) three times to catch the race.

### SCH-03: the Tools badge clears after the upload is deleted (two devices)

**Proposed case:** `ScheduleDistTwoUserTest.SD_70_badgeClearsAfterDelete`. Built the way `homechat/TwoUserChatTest.java` sets up A and B (`ZILLIT_RECEIVER_UDID`). Skip without B.

**Steps:**
1. On B: open the Tools tab. Read the `badge` of the row whose `nameTv` is "Schedule Full & One Line" (`item_tools_section_row.xml:50`) → b0, where empty counts as 0.
2. On A: upload a page `ZL_AUTO_<run>_badge` (the SD-35 helper).
3. On B: wait until the badge is b0+1 or more. This is a control step that proves badges work.
4. On A: delete that page (the SD-44 flow).
5. On B: go back to Tools and pull to refresh, or leave and re-enter. Wait up to 30 s.

**Expected:** B's badge is back to b0, or hidden when b0 = 0.

### SCH-04: the folder leaves Pages once its last page is deleted

`SCHEDULE_DIST_MODULE_TESTCASES.md` SD-44 already says this. The code, `ScheduleDistTest.pageDelete` (ScheduleDistTest.java:553-568), checks only that the page left the folder.

**Proposed case:** extend SD-44, or add `SD_45b_lastPageDeleteRemovesFolder` with its own one-page scene.

**Steps:**
1. Upload one page for scene `99ZLAUTO<run>L`.
2. Open its folder → ⋮ → Delete → Yes. Wait for the snackbar and tap its action ("Ok") if there is one.
3. Without leaving the tool, read the folder screen, then go back to the Pages tab and read `settledFolders()`.
4. Tap the scene row if it is still there.

**Expected:**
- The folder screen closes or shows empty.
- Back on Pages, no folder is titled "Scene No. 99ZLAUTO<run>L".
- If the row is still there (the bug), tapping it must not list the deleted page.
- Report it as failing if the row only disappears after a tab switch.
