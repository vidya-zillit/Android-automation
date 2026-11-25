# Box Schedule (Production Diary): sheet bugs vs Android suite

Web input: `box-schedule.md` (0 web tests, 3 sheet bugs).
Android suites: `tests/box/` (12 classes). Docs: `BOX_MODULE_TESTCASES.md`, `BOX_IDS_AND_ROUTES.md`.
App source: `Zillit-Android/.../bottomNav/tools/new_boxschedule/ui/boxschedule/`, called `nbs/` below.

## Summary

| Bug | Title | Status | Android case / note |
|---|---|---|---|
| BOX-01 | Search filters the calendar and the List view differently | **MISSING** | The two views use **different search rules in the Android source too**. No test compares them. `etListSearch` is only a helper in `BoxListPage` |
| BOX-02 | Another member's History shows the title of a Personal Note | **MISSING** | History is never opened by a test (BS-08 only checks that `drawerHistory` exists). Needs two users |
| BOX-03 | History shows a schedule's dates one day early | **MISSING** | The dates come from the server (`item.details`), so Android shows the same text |

Counts: 3 MISSING, 0 COVERED, 0 WEB-ONLY, 0 CONFLICT.

The doc's planned `BoxViewsTest` (FL-, DD-, PD-, HS-) was never written. All three bugs belong there.

---

## MISSING, in full

### BOX-01: search gives the same answer in Calendar and List

How the two views search, from the source:
- The search text is shared between them (`BoxScheduleViewModel.filterSearchText`, BoxScheduleViewModel.kt:243-245).
- **Calendar** (CalendarFragment.kt:349-420): matches a schedule on `title` / `typeName`, and also matches **events** on title, description and notes, and **notes** on title, description and notes. Each is filtered on its own.
- **List** (`ListFragment.filterDays`, ListFragment.kt:219-229): matches a **day** only on `title`, `typeName` or its short dates. It never looks at event or note text. A day whose only match is an event title is dropped; a matching day keeps all its events and notes, matching or not.
- For a query that matches nothing, both views end up empty. The List shows `emptyState` with "No schedule days yet" / "Click "Add Schedule"…" (`no_schedule_title` / `no_schedule_desc`, strings.xml:5117-5118), **not "No Results"**. That is a wording mismatch with the web expectation.

**Proposed case:** `BoxViewsTest.FL_10_searchAgreesAcrossViews`

**Steps:**
1. Fixture: create an event `ZL AUTO evt-search <run>` on a day that also has a schedule of a different type. Reuse the `BoxEventLifecycleTest` EN-01 helpers.
2. Calendar → Month. Type `zzqqnomatch` into `etCalendarSearch`.
3. Check that no visible cell has children in `pillContainer` or `eventContainer`.
4. Switch to List (`btnListView`). Check that `etListSearch` already holds `zzqqnomatch`, and that `emptyState` is shown.
5. Change the search to `evt-search <run>`.
6. Calendar: the fixture's day has an event pill and **no** schedule pill (`titlesOn(day)` contains the event).
7. List (By Date): expect a row for that day showing the event.

**Expected:**
- Step 4: both views are empty. The List should say "No Results" (today it says "No schedule days yet", so pin it as a known wording bug).
- Step 7: **fails today**. The List drops the day because `filterDays` ignores event text. That is the Android form of BOX-01.

**Cleanup:**
1. Clear the search in both views.
2. Delete the event by its fixture name (BoxCleanupTest CU-03 already sweeps `ZL AUTO`).

### BOX-02: a Personal Note leaves no trace in another member's History

- `HistoryActivity` filters the server's activity log only by action and date (HistoryActivity.kt:231-236). It shows `targetTitle` as is (:287).
- If the server returns a personal-note row to member B, Android shows it.

**Proposed case:** `BoxHistoryTwoUserTest.HS_08_personalNoteHiddenFromOthers`. Two devices, built the way `homechat/TwoUserChatTest.java` sets them up (`ZILLIT_RECEIVER_UDID`). Skip without device B.

**Steps:**
1. On A: drawer → `drawerCreateNote`. Make it a **Personal** note titled `ZL AUTO personal <run>` (NT-04 path) and save.
2. On B: Box Schedule → `btnMenu` → `drawerHistory`.
   - Filter: action "ALL", no date (`btnClearDate`).
   - Scroll `historyList` to the end, reading `tvHistoryTitle`, `tvTargetType` and `tvActionLabel` on each card.
3. On A: open History too.

**Expected:**
- On B: no card has `tvHistoryTitle` containing `ZL AUTO personal <run>`, and there is no `ADDED` · `Note` card from A at that time.
- On A: the card **is** listed. This is the control, proving the row exists.

**Cleanup:** on A, delete the note (the CU-03 sweep).

### BOX-03: History shows a schedule's own dates

- The CHANGED · SCHEDULE card text is `item.details`, sent by the server, and rendered as is (HistoryActivity.kt:288-290).
- `tvDateTime` is formatted locally with `DateUtils.formatDate(item.createdAt)`.
- So a server timezone bug shows on Android. The device must be in IST (+05:30), as in the sheet.

**Proposed case:** `BoxViewsTest.HS_09_historyShowsScheduleDates`

**Steps:**
1. Create a Prep schedule for a 3-day range D…D+2, where D is ten or more days ahead, via SD-04 "Date Range by number of days" = 3. Note D and D+2 as "MMM d".
2. Drawer → `drawerHistory`.
3. Find the newest card where `tvTargetType` = "Schedule" and `tvActionLabel` is "ADDED" or "CHANGED".

**Expected:**
- Its `tvDetails` contains D's and D+2's "MMM d" text, for example "Nov 28" and "Nov 30".
- It does not contain D-1.
- Any "N day(s) added" figure equals 3.

**Notes:**
- Record the device timezone in the log: `driver.getDeviceTime()` gives the offset.
- Clean up through the ED-10 or CU-02 path.
