# Crew DOOD — test cases and findings

**Crew DOOD** is the crew's *Day Out Of Days*: every project member, grouped by department,
with a bar of the days they are booked (SW / W / WF / SWF) across the production's Prep /
Shoot / Post, a day-by-day detail per person, the project's **Days off & holidays** calendar,
and an **Export** of the DOOD workbook. Tools → **Crew DOOD** (identifier `crew_dood_tool`,
unit name `crew_dood_label`, group PRODUCTION on the automation project) opens
`bottomNav/tools/crewdood/CrewDoodActivity`. Web counterpart: `/film-tools/crew-dood`
(`zillit_web/src/pages/FilmTools/crewDood/`, rules in `lib/dood.js`, which the Android
`CrewDood` model ports).

> **Read-only for crew data.** People come from the crew list
> (`project:user:*` events re-read it), bookings from deal memos (`deal:*` events re-read
> them). The tool writes exactly one thing: the project-wide **Days off & holidays**
> calendar (`PUT {dealmemo}/crew-dood/calendar`, which *replaces* it whole). There is no
> create / edit / delete of crew rows here.

**Status: automated and run on the Pixel 7 (2026-11-23, project ZL-Android-Automation, dev
build Zillit_32535-dev).** Suite
[`CrewDoodTest`](src/test/java/com/zillit/automation/tests/crewdood/CrewDoodTest.java)
(base `CrewDoodBaseTest`), `TestOrder.CREW_DOOD = 176`, page objects `CrewDoodPage` and
`CrewDoodCalendarPage` (on the shared `VisiblePage` base). Last full run: **28 cases — 24
passed, 0 failed, 4 skipped, 9 minutes**; CD-99 left no `ZL_AUTO_` holiday behind. The skips
are data / setup the project lacks, not defects: CD-14 and CD-30 (no deal memos — the export
is refused with the server's "No matching deal memos found."), CD-40 (needs a second admin to
revoke rights live), CD-41 (needs network fault injection). **No app defect found.** One run:

```bash
./run-and-report.sh --only CrewDoodTest
```

(`./run-ui.sh --crew-dood` once the coordinator wires the flag; the IDE entry
`.run/Zillit_Crew_DOOD.run.xml` already calls it.)

The automation project's data: two departments (WRITER — *Vidya Pixel*, Screen writer;
PRODUCTION — *Vidya Testing*, Production coordinator), both "0/1 booked", no production dates,
weekly off Sat + Sun, no holidays.

**How to read this sheet.** Every case says what a user does and what must then be true.
Quoted wording is the app's own (`res/values/strings_crew_dood.xml`), because the automation
matches on it; `id` in backticks is a real resource id. There is no Crew DOOD tab on the QA
sheet (see *From the QA sheet*), so every case comes from the source, the web tests and what
the device showed.

## The shape of the data

- **Rows**: one header per department that has people (`item_crew_dood_dept`: `deptName`,
  `deptCount` = "`%1$d/%2$d booked`", `deptChevron`), then its people
  (`item_crew_dood_crew`: `avatar`/`avatarInitials`/`avatarPhoto`, `name` — "`TBC`" when
  blank —, `role` — hidden when empty —, `days` = "`–`" or "`N day(s)`", `dayBar`).
  The count is over *everyone* in the department and **filters never change it**.
- **Dates**: every booking and shoot day, widened to Monday–Sunday weeks, capped at 1100
  days. With nothing booked: **this week's Monday to today + 27 days**, widened.
- **Phases** (the `phaseBand` above the list and the detail's day labels "`Shoot Day %d`",
  "`Prep W%d`", "`Post W%d`", "`Rest day`") exist only when the project has production
  dates. The automation project has none: no band, blank labels.
- **Calendar**: `weekly_off` (0 = Sunday … 6 = Saturday; default Sat + Sun) and one
  `{date, name}` per holiday day. Consecutive days with the same name show as one row.
  Nobody is booked on a day off or a holiday.

**The id trap.** The person detail and the editor are fragments *added* into
`detailContainer` on top of the list; `listPane` stays in the accessibility tree underneath.
`name`, `role`, `avatar` exist in both, and the list's `days` TextView shares its id with the
detail's `days` RecyclerView. Every lookup in `CrewDoodPage` is scoped to one pane by XPath.

**The month grid has no day elements.** `CrewDoodMonthView` draws the days itself; a tap is
resolved from coordinates (28dp header, Monday-first rows of 44dp + 4dp gap, 7 columns).
`CrewDoodCalendarPage.tapDay` does the same maths, taking the density from the view's own
height, and scrolls the editor when the draft panel would cover the row.

## 1. Shell · `CD-0`

| ID | Steps | Expected |
|----|-------|----------|
| CD-01 | Tools → search "DOOD" → tap **Crew DOOD** | `CrewDoodActivity` opens; toolbar `headerText` "CREW DOOD"; `search` shows hint "`Role or name`"; `deptLabel` "`All depts`"; "`Days off & holidays`" (`calendarButton`) and "`Export`" (`exportButton`) both enabled once data loaded |
| CD-02 | Read the whole list | Starts with a header; every header reads "n/m booked", m = people listed under it, n = those whose day count is not "–"; every person has a name (or "TBC") and "–" / "N day(s)" |

## 2. Search & department filter · `CD-0`

| ID | Steps | Expected |
|----|-------|----------|
| CD-03 | Type a lower-case piece from the middle of a name | That person is listed; everyone listed contains the text in "role name" (case-insensitive *contains*) |
| CD-04 | Type an upper-case piece of a role | The person with that role is listed |
| CD-05 | Type text nothing matches; then clear | `stateText` "`No crew match your filters.`", no rows, no `retry`; clearing restores every row and the hint |
| CD-06 | Search an exact name | Departments still shown keep their "n/m booked" exactly |
| CD-07 | Tap `deptButton` | Bottom sheet "`Department`" lists "`All departments`" + every department in list order; `optionCheck` on "All departments"; BACK closes it, filter unchanged |
| CD-08 | Pick the last department; reopen; pick All departments | Only that department listed, `deptLabel` = its name, the sheet checks it; All departments → "`All depts`" and everyone back. *Needs ≥ 2 departments.* |
| CD-09 | Tap a department header twice | First tap: its people disappear, the header stays (chevron −90°); second tap: they come back |
| CD-10 | Pull the list down | It reloads (`refresh`) and lists exactly the same rows |

## 3. Person detail · `CD-1`

| ID | Steps | Expected |
|----|-------|----------|
| CD-11 | Tap a person; BACK | Detail header: `dept`, `name`, `role` equal the row's; key (`key` ChipGroup) reads "SW Start work · W Working · WF Work finish · SWF One day · OFF Off · HOL Holiday"; BACK returns to the list, still in Crew DOOD |
| CD-12 | Open a person | Opens scrolled to **today**; month headers "`MMMM yyyy`" + "`%d working`" ("0 working" for an unbooked person); every day has a weekday MON…SUN |
| CD-13 | Open a person | With production dates: labels are Shoot Day n / Prep Wn / Post Wn / Rest day; without (no `phaseBand`): labels blank except holidays |
| CD-14 | Open a booked person, scroll the months | Sum of the months' "N working" = the list's "N days". **Skipped: no deal memos with dates on the project.** |

## 4. Days off & holidays · `CD-2`

| ID | Steps | Expected |
|----|-------|----------|
| CD-20 | Tap "Days off & holidays" | Title "`Days off & holidays`", hint "`One calendar for the whole project. Nobody is booked on a day off or a holiday — not even a shoot day.`", `save` shown but **disabled**; `weekSummary` matches the selected toggles ("`Sat, Sun off · 5-day week`"); `weekHelp` "`Tap a weekday to select or deselect it as a weekly day off`"; grid opens on this month; `holidayCount` "`N added`" or `holidayEmpty` "`No holidays yet. Tap dates on the calendar to add one.`" |
| CD-21 | Tap a working weekday; tap it again | Marked off, summary updated, Save on; back to the loaded week → Save off again. Closed **without saving** |
| CD-22 | Turn every weekday off | `weekHelp` "`Leave at least one working day in the week.`" (red), summary "`0-day week`", Save off; one day back on → help text restored. Not saved |
| CD-23 | Tap day A; tap A − 3; tap A + 1; Clear | `draftRange` "`EEE d MMM yyyy`", `draftCount` "`1 day · tap end date`"; backward range → "`d – d MMM yyyy`", "`4 days`"; third tap starts a new one-day selection; Clear hides `draft`; Save stays off |
| CD-24 | Tap a working day ~10 days out, name it `ZL_AUTO_<run>_HOL`, Add holiday, Save; reopen | Row: name, `meta` "`<label> · 1 day`", tile "`MMM`"/day; count "+1 added"; Save on → editor closes (toast "`Days off & holidays saved.`" or the server's own text); on reopening the holiday is there and Save is off |
| CD-25 | Open a person, find that date | The day's `dayLabel` reads the holiday's name |
| CD-26 | Select the same day, Add holiday | `draftError` "`Every day in that range is already a holiday.`"; nothing added |
| CD-27 | Select a 401-day range, Add holiday | `draftCount` "`401 days`"; `draftError` "`A range can be at most 366 days.`" |
| CD-28 | Select a 3-day range, name it, Add; Close without Save; reopen | One row "`<range> · 3 days`", tile "d1–d3"; after Close + reopen it is **gone** (only Save writes) |
| CD-29 | Rename the saved holiday in its row, Save; reopen | Save turns on with the rename; the new name is kept, the old one gone |

## 5. Export · `CD-3`

| ID | Steps | Expected |
|----|-------|----------|
| CD-30 | Tap Export (**skips** when the server answers "No matching deal memos found." and nobody is booked) | `exportProgress` while it builds; then the `.xlsx` opens in a viewer (or is saved to Downloads with a toast of its file name), plus "`N deals were left out of the export because they have no start or end date.`" when the server says so; never "`Couldn't export the Crew DOOD.`" |

## 6. Not automated here · `CD-4`

| ID | Case | Why |
|----|------|-----|
| CD-40 | View access taken away while inside → toast "`You no longer have access to Crew DOOD.`" and the tool closes | Needs a second admin to change the right live; the second device is reserved for other modules today |
| CD-41 | A failed load → "`Couldn't load the Crew DOOD. Check your connection and try again.`" + "`Try again`" | Needs network fault injection |
| — | Bars and SW/W/WF/SWF badges, booked counts > 0, the phase band | Need deal memos with dates and production dates on the project; the automation project has neither (CD-14 skips; CD-13 checks the "no dates" branch) |
| — | Live updates (`deal:*`, `crew_dood_calendar:updated`, `project:user:*`) | Need a second writer at the same time |
| — | Save refused by the server (no rights) → "`Couldn't save the days off & holidays. Try again.`", editor stays open | Needs a non-admin account |

## 7. Cleanup · `CD-99`

Opens the editor, removes every holiday whose name starts with `ZL_AUTO_`, puts the weekly
days off back to what CD-20 first read if they differ, saves, and reopens to check nothing
`ZL_AUTO_` is left. Weekly days off are never saved by any other case.

## From the QA sheet

The module testing report has **no Crew DOOD tab**, no Crew DOOD row in *Summary*, and no
row mentioning "DOOD" / "Day Out" in *By platform* or *Crew List* (checked 2026-11-23). The
*Crew List* tab's search bugs (CREW-05 prefix-only search) do not apply here: Crew DOOD's
search is its own (`CrewDoodViewModel.rowsFor`, a case-insensitive *contains* over
"designation name"), covered by CD-03 / CD-04.

## From the web E2E report

Source: `web-e2e/crew-dood.md` and `web-e2e/crew-list.md` (Zillit E2E Report, 2026-11-17,
dev.zillit.com).

| Web item | What it is | Here |
|----------|------------|------|
| `crew-dood.md` — web test cases | **none** (coverage: none) | — |
| `crew-dood.md` — sheet bugs | **none** | — |
| `crew-list.md` — web test cases | **none** | — |
| CREW-01 (sheet) | Crew List member contact sheet misses the Zillit e-mail | **web/Crew-List-only** — Crew DOOD has no contact sheet; a person opens the DOOD detail (dept / name / role / days), not a profile |
| CREW-02 (sheet) | Empty add-external-user form gives no clear error | **web/Crew-List-only** — Crew DOOD has no add-user form; it lists the crew list read-only |

With nothing in the E2E report, the closest web reference is the web repo's own component
test, `zillit_web/src/pages/FilmTools/crewDood/__tests__/CrewDood.test.jsx` (the Android
port follows the same rules). Mapping of its cases:

| Web component test | Here |
|--------------------|------|
| lists people in the crew list order; deal-only people at the end of their department | CD-02 (order not asserted: no deal-only people on the project) |
| draws start/work/finish bars from active deals, skipping the weekend, counts booked days; deactivated deals; one-day booking = SWF | CD-14 (skipped: no deal memos) |
| department booked count and the phase band | CD-02 (count), CD-13 (band absent branch) |
| narrows to booked crew, and by search | CD-03, CD-04, CD-05 (Android has no "booked only" filter) |
| narrows to one department | CD-07, CD-08 |
| collapses a department when its row is clicked | CD-09 |
| groups weeks / hover card / photo signing / grid scroll | web-only (desktop grid + hover) |
| offers a retry when the crew list fails | CD-41 (skipped: needs fault injection) |
| still draws the grid without phases when the calendar fails | not automated (fault injection) |
| draws the saved calendar: a holiday is never worked and named | CD-24, CD-25 |
| calendar saved elsewhere applied from the socket / re-read / crew re-read per burst | not automated (needs a second writer) |
| exports the workbook with a loader and the skipped count; names the file; shows the server message on refusal | CD-30 |
| calendar: sets weekly days off and a one-day holiday, saves, redraws | CD-21 (toggle, not saved), CD-24 |
| calendar: range in either direction, listed as one row | CD-23, CD-28 |
| calendar: clears a selection without closing, removes a whole range | CD-23 (Clear), CD-99 (remove) |
| calendar: keeps a working day in the week; refuses days that are all holidays | CD-22, CD-26 |
| calendar: stays open with the server message when the save fails | not automated (needs a non-admin) |
| calendar: keeps edits when someone else saves / "Load theirs" | web-only (Android has no "Load theirs"; an untouched editor follows, an edited one keeps its edits) |
| sends the user out when view rights are revoked | CD-40 (skipped) |

## Findings

No product defect. What the runs and the source showed:

1. **Export on a project with no deal memos is refused, with the server's text.** The tap
   shows the spinner, then the toast "No matching deal memos found." and no file. Web shows
   the server's message the same way. Opening an empty workbook might be friendlier, but this
   is the designed path (`CrewDoodRepository.exportDood` → `Failed(message)`), so CD-30 skips
   rather than fails. The workbook itself is not checked until the project has deal memos.
2. **The department sheet and the list spell department names differently.** List headers
   are `textAllCaps` ("WRITER"); the sheet and the filter label show the name as typed
   ("Writer", "Production"). This is styling, not a bug, and the suite compares them
   ignoring case.
3. **Accessibility (low): the holiday grid cannot be used with TalkBack.**
   `CrewDoodMonthView` draws every day itself and exposes no virtual views
   (no `ExploreByTouchHelper`), so a screen-reader user cannot pick a date, and automation
   has to tap by geometry. The weekday toggles do carry content-descs (Monday…Sunday).
4. **Accessibility (low): the list stays reachable under the detail and the editor.** Both
   are fragments added on top of `listPane` without hiding it from accessibility, so the
   tree (and TalkBack) still sees the crew rows, the search box and the buttons behind the
   open screen.
5. **Save echo.** A successful Save also triggers the app's own `crew_dood_calendar:updated`
   socket event, which re-reads the calendar once more (seen in logcat: PUT, the socket frame,
   then a GET). Harmless; noted for whoever looks at request counts.
6. The server's success message is the bare key `crew_dood_calendar_saved`. Where the label
   table has no translation for it, the app falls back to its own
   "Days off & holidays saved." as designed (`CrewDoodActivity.serverText`).

## Screens and ids

- `activity_crew_dood.xml`: `toolbar` (`headerText`), `search`, `deptButton`/`deptLabel`,
  `calendarButton`, `exportButton`/`exportIcon`/`exportProgress`, `phaseBand`, `refresh`,
  `list`, `stickyHeader`, `skeleton`, `stateBox`/`stateText`/`retry`, `listPane`,
  `detailContainer`.
- `sheet_crew_dood_dept.xml` / `item_crew_dood_dept_option.xml`: `options`, `optionLabel`,
  `optionCheck`.
- `fragment_crew_dood_detail.xml`: `avatar`, `dept`, `name`, `role`, `key`, `days`
  (RecyclerView), `stickyMonth`; rows `item_crew_dood_month` (`monthTitle`, `monthWorking`)
  and `item_crew_dood_day` (`dayRoot`, `dayNumber`, `weekday`, `dayLabel`, `badge`).
- `fragment_crew_dood_calendar.xml`: `close`, `save`, `saving`, `scroll`, `content`,
  `weekSummary`, `weekDays` (7 TextViews, content-desc Monday…Sunday, `selected` = off),
  `weekHelp`, `monthTitle`, `prevMonth`, `nextMonth`, `monthView`, `offSwatch`,
  `holidayCount`, `holidayList`, `holidayEmpty`, `draft`, `draftRange`, `draftCount`,
  `draftName`, `draftError`, `draftClear`, `draftAdd`; holiday rows
  `item_crew_dood_holiday` (`tileMonth`, `tileDay`, `name` EditText — hint "`Add a name`",
  `meta`, `delete` — content-desc "`Remove <label>`").
