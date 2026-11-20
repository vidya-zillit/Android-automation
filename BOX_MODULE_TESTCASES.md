# Production Diary / Box Schedule — test cases

One module, two names: `strings.xml` calls it **Production Diary/Box Schedule** (`bs_title`).
Live code is `bottomNav/tools/new_boxschedule`, opened from Tools as `BoxScheduleActivity`.
The older `bottomNav/tools/boxschedule` belongs to **pre-production** and is a different
feature — do not automate it by mistake.

Screens and ids: [BOX_IDS_AND_ROUTES.md](BOX_IDS_AND_ROUTES.md).

**How to read this sheet.** Every case says what a user does and what must then be true.
Where the app's own wording matters it is quoted exactly, because the automation matches on
it. `id` in backticks is a real resource id, read from the layout.

---

## The shape of the data

```
ScheduleType          Shoot · Prep · Day Off · Travel …   (colour; some systemDefined)
   └── ScheduleDay    a date, or a set of dates, carrying one type
         ├── events   eventType = "event"
         └── notes    eventType = "note"   (general or personal)
```

An event or note may **link** to a schedule day, but does not have to — the link is a
convenience for picking a date, not ownership. Code: *"Only attach scheduleDayId when the
user explicitly linked one."*

---

## 1. Module shell · `BS-`

| # | Case | Expected |
|---|---|---|
| BS-01 | Tools → Production Diary/Box Schedule | `BoxScheduleActivity` opens; toolbar title; `tvPrepared` shows "Prepared: …" |
| BS-02 | Calendar ↔ List | `btnCalendarView` / `btnListView` swap the fragment in `fragmentContainer` |
| BS-03 | Calendar sub-modes | `btnMonth` / `btnWeek` / `btnDay` each render; selection sticks while the screen is open |
| BS-04 | List sub-modes | `btnByDate` / `btnBySchedule` each render |
| BS-05 | Set as Default — Calendar | `btnSetDefaultView` → choose Calendar → reopen the module → it opens on Calendar |
| BS-06 | Set as Default — List | Same, choosing List. **The two halves must be separate cases**: a default that only ever gets set to the value already in use proves nothing |
| BS-07 | Default survives an app kill | Stored in `SharedPref`, not in the Activity |
| BS-08 | Drawer opens with all its actions | `btnMenu` → Create Schedule · Create Event · Create Note · Edit Types · Presets · Share · Print · Distribute to DD · History |
| BS-09 | History badge | `drawerHistoryBadge` shows a count when there are unseen revisions |
| BS-10 | Back from the module | Returns to Tools, never out of the app |
| BS-11 | Legend | `legendContainer` lists the schedule types in use with their colours |

## 2. Schedule types · `TY-`

| # | Case | Expected |
|---|---|---|
| TY-01 | Drawer → Edit Types | `TypeManagerActivity` opens and lists existing types |
| TY-02 | Create a type | Name + colour → it appears in the list |
| TY-03 | Create a type from the schedule form | The create-schedule page offers the same thing inline (`newTypeForm`) **and** a way through to the manager — a type made here is selectable immediately, without reopening the form |
| TY-04 | Rename a type | The new name shows wherever that type is used, including the legend |
| TY-05 | Delete a type | Confirmation, then it is gone from the list and from the type picker |
| TY-06 | A system type | `systemDefined` types: establish whether delete is offered at all. **Unverified — needs the owner's answer** |
| TY-07 | Colour | The colour chosen is the colour drawn on the calendar pill |

## 3. Creating a schedule day · `SD-`

| # | Case | Expected |
|---|---|---|
| SD-01 | Drawer → Create Schedule | Form opens with a type picker and the three date tabs |
| SD-02 | No type chosen | Save refused: *"Please select a type"* (`cs_error_select_type`) |
| SD-03 | No dates chosen | Save refused: *"Please select dates"* (`cs_error_select_dates`) |
| SD-04 | Date Range by number of days | Start + `etNumberOfDays` → that many consecutive days |
| SD-05 | Date Range by end date | `btnByEndDate` → start + end; the span is inclusive |
| SD-06 | Calendar tab | Tap individual dates, **including non-adjacent ones**; chosen dates show as tags |
| SD-07 | Day Wise tab | Weekday filtering over a span produces only those weekdays |
| SD-08 | Past dates | Disabled — a past date cannot be chosen |
| SD-09 | Start date locked from a calendar cell | Opening the form from a cell pins the start date |
| SD-10 | Save | The day appears on the calendar with the type's colour, and in List view |
| SD-11 | Edit a saved schedule | Reopens with type and dates filled, on the right tab |
| SD-12 | Delete a schedule | Confirmation, then gone from calendar and list |

## 4. Conflict — the Prep ∕ Day Off case · `CF-`

The heart of the module. A schedule that lands on dates already carrying one raises
**HTTP 409**, and `ConflictDialog` offers three ways out, with the count of clashing dates.

| # | Case | Expected |
|---|---|---|
| CF-01 | Overlapping dates raise the dialog | Title, and the message naming how many dates clash |
| CF-02 | All three options are offered | **Replace** (marked Recommended) · **Extend** · **Overlap**, each with its description |
| CF-03 | Back | `btnBack` returns to the form with the input intact — nothing saved |
| CF-04 | **Replace** | Prep is removed from those dates; Day Off takes them |
| CF-05 | **Extend** | Prep survives and is pushed out at the end to make room |
| CF-06 | **Overlap** | Both sit on the same dates |
| CF-07 | **ZL-19566 — scattered dates must not swallow the gaps** | Prep over a long span, then Day Off on three scattered dates (e.g. 17 / 26 / 29) → Replace → **only those three change**. Every date mode sends `by_days` precisely so the server cannot expand the span; `by_dates` used to wipe all thirteen days between. Was Android-only — web and iOS always sent `by_days` |
| CF-08 | Single-day change from a cell | Changing one day's type asks Replace/Extend/Overlap **up front**, not after a 409 |
| CF-09 | Same type on the same day | No API call, no error — the code returns early when the type has not changed |

## 5. Events · `EV-`

| # | Case | Expected |
|---|---|---|
| EV-01 | Drawer → Create Event | Opens on the **event** tab |
| EV-02 | Title required | Save refused without one |
| EV-03 | Link a schedule day | `spinnerLinkDay` lists the days that have schedules; picking one moves the event's date to it |
| EV-04 | The link is optional | An event saves with no schedule day — it is a reminder, not a child |
| EV-05 | Times | Start and end; **overnight stays on the next day** after linking a schedule day (ZL-21366) |
| EV-06 | Full day | Hides the time pickers |
| EV-07 | Repeat | `repeatStatus` + `repeatEndDate`; the occurrences appear on the calendar |
| EV-08 | Reminder | Saved and shown on the event |
| EV-09 | Location | Saved, with coordinates where picked |
| EV-10 | Colour | The chosen colour is what the calendar draws |
| EV-11 | **Home Calendar prompt** | On save in **create** mode: *"Would you like to set a reminder for this event on your Home Calendar?"* — Yes / No. **Not dismissable by tapping outside** (`setCancelable(false)`) |
| EV-12 | Yes → it reaches the Calendar module | The event is visible in Calendar as well |
| EV-13 | No → it stays here | Not in Calendar |
| EV-14 | **Edit must not re-ask** | Editing an existing event keeps its old flag and shows no prompt — deliberate, "to avoid accidental flips on minor edits" |
| EV-15 | **Edit shows the linked day** | Reopening an event that is linked must show that day in `spinnerLinkDay`. **Reported broken.** The code has `syncLinkDaySpinnerSelection()` but drives it from two places and admits "whichever lands last wins" |
| EV-16 | Distribution | self / users / departments / all departments / presets |
| EV-17 | External guests | Emails outside the org can be added on top of any distribute mode |
| EV-18 | Delete a recurring event | Asks **this occurrence** or **all occurrences** |
| EV-19 | Delete a single event | Confirmation, then gone from calendar, list and day detail |
| EV-20 | Call type | audio / video / meet in person; a call event offers Join |

## 6. Notes · `NT-`

| # | Case | Expected |
|---|---|---|
| NT-01 | Drawer → Create Note | Opens on the **note** tab |
| NT-02 | Link a schedule day | Same picker as events; it moves the note's date pill too |
| NT-03 | **General** note | Offers distribution — only the chosen people see it |
| NT-04 | **Personal** note | No distribution; private to its author |
| NT-05 | Default type | A note with no type set reads as **general** |
| NT-06 | Note types come from the server | `getNoteTypes()`, not a hard-coded list |
| NT-07 | Edit a note | Reopens with its type and text |
| NT-08 | Delete a note | Gone from the day |
| NT-09 | Rights | Creating a note needs posting access, same as events and schedules (changed by ZL-19571 — viewers could once do it) |

## 7. Filters · `FL-`

| # | Case | Expected |
|---|---|---|
| FL-01 | Filter sheet opens | Shows the filters currently applied, not a blank sheet |
| FL-02 | By content: schedules | Only schedules remain |
| FL-03 | By content: events | Only events |
| FL-04 | By content: notes | Only notes |
| FL-05 | By type | Only days of that type remain |
| FL-06 | Both at once | Content **and** type narrow together |
| FL-07 | Count badge | Shows how many filters are active |
| FL-08 | Clear | Everything comes back |
| FL-09 | Filters carry across views | Applied in Calendar, still applied in List — the state is shared |

## 8. Day detail · `DD-`

| # | Case | Expected |
|---|---|---|
| DD-01 | Tap a calendar date | Opens that day, listing its schedules, events and notes |
| DD-02 | Row actions | Schedules: Edit + Delete · Events: Edit + Remove · Notes: Delete |
| DD-03 | Edit from the day | Opens the right form, filled |
| DD-04 | Delete from the day | Removes it, and the calendar follows |
| DD-05 | An empty day | Reads as empty rather than showing a blank list |

## 9. Share · Print · Distribute to DD · `PD-`

All three generate the same PDF and ask the same two questions first.

| # | Case | Expected |
|---|---|---|
| PD-01 | The chooser appears | Layout: **Calendar grid** or **List** |
| PD-02 | Personal notes | The chooser asks whether to include them (`includePersonalNotes`) |
| PD-03 | Share | Hands off to the system chooser; the module survives the return |
| PD-04 | Print | Opens the print flow |
| PD-05 | Distribute to DD | Pushes the PDF into the Document Distribution library |
| PD-06 | DD needs its own right | Without DD posting access the action is refused — a right in another module gates this one |
| PD-07 | Personal notes excluded | A personal note must not appear in a PDF generated with the option off |
| PD-08 | Download right | Share/Print/Distribute are gated by `hasDownloadAccess` |

## 10. Presets · `PR-`

| # | Case | Expected |
|---|---|---|
| PR-01 | Drawer → Presets | Lists existing presets |
| PR-02 | Create a preset | Named, with its people; appears in the list |
| PR-03 | Edit a preset | PUTs to `user-preset/{id}`; title and save button change wording |
| PR-04 | Use a preset | Choosing `presets` as the distribute mode offers them |
| PR-05 | Delete a preset | Gone from the list and from the picker |

## 11. History · `HS-`

| # | Case | Expected |
|---|---|---|
| HS-01 | Drawer → History | Revisions and the activity log |
| HS-02 | A revision carries its detail | Number, colour, description, who changed it, when |
| HS-03 | Filter by action | ALL · Added · Changed · Removed · Shared · Printed |
| HS-04 | Filter by date | Narrows to that date |
| HS-05 | Printing is recorded | Print, then find it in history as a `printed` action |
| HS-06 | Sharing is recorded | Same, as `shared` |
| HS-07 | Both filters at once | Date **and** action |

## 12. Rights · `RT-`

| # | Case | Expected |
|---|---|---|
| RT-01 | View only | The module opens and reads; create actions are absent |
| RT-02 | No posting access | Create Schedule / Event / Note refused |
| RT-03 | No download access | Share / Print / Distribute refused |
| RT-04 | Posting access | All three create paths work |

*The RT- series needs a second, view-only account — the same gap the Map module has.*

---

## Known defects, both already admitted in the code

1. **ZL-19566 — Replace over scattered dates.** Covered by **CF-07**. Every date mode now
   sends `by_days`; `by_dates` let the server fill the whole span, so a three-date Day Off
   could wipe thirteen days of Prep. Android-only; web and iOS always sent `by_days`.

2. **The linked schedule day is missing when editing an event.** Covered by **EV-15**. The
   owner reports it; the code's own comment concedes the race.

## Open questions for the owner

- **TY-06** — can a `systemDefined` type be deleted, or only the ones a project made?
- Does a **revision** get cut on every change, or only when the schedule is shared?
- **CF-** — does conflict apply to events too, or only to schedule days? Only days raise a
  409 in the code I have read.

## What the app actually demands — found by running it

Six validation rules stood between a test and a saved record. None is in the sheet above
because none is visible from the UI; each was found by a case failing and then reading the
code. They are listed here because anyone writing more cases will hit them too.

| To save… | The app also requires | Where |
|---|---|---|
| an **event** | a title of **3+ characters** | `CreateEventActivity:1657` |
| an **event** | start and end times that **differ** — a new event opens with them equal, so a save with only a title is refused with "end time before start". Full-day sidesteps it | `:1684` |
| an **event** | a **Distribute To** value (or an external guest). Tests use **"Only Me"**, which reaches nobody | `:1850` |
| a **new event** | a start date **not in the past**. Edit mode is exempt, so past events stay fixable | `:1678` |
| a **schedule** | a type AND at least one date | `cs_error_select_type`, `cs_error_select_dates` |
| a **type** | a colour **no existing type is using** — and the Add form always opens on the same hard-coded `#F39C12`, so in a project whose types already cover it, Add can never succeed until the swatch is driven. The refusal names the type holding the colour | `TypeManagerActivity:191-195`, `tm_color_used` |
| a **type** | a name no existing type has, compared **case-insensitively**. This gate comes *before* the colour one, so a duplicate name always reports the name | `TypeManagerActivity:186-189` |

### Behaviour that breaks a naive test

| What the app does | Why it trips a test | Where |
|---|---|---|
| The **type manager closes itself** 150ms after the server confirms a new type | The list cannot be re-read after a successful create — the screen is gone. Its closing *is* the success signal; the list must be checked on a fresh visit | `TypeManagerActivity:134-139` |
| The **colour wheel** changes hue on `ACTION_MOVE` only — `ACTION_DOWN` just records whether the press was central, and `ACTION_UP` fires only for a central press | A tap can never pick a colour. It needs a drag along the ring, outside the 40px centre, with a stream of MOVE events | `ColorPickerView:64-97` |
| The **date-chip count label** is `""` until a date is picked, and sits below the chips in a `ScrollView` | Empty or scrolled-out views leave the accessibility tree, so the label reads as absent whether nothing is picked or it merely moved. The chips are the evidence | `CreateScheduleActivity:510`, layout `:185` |

### Screens built in code, with no ids at all

Three places in this module build their rows in Kotlin rather than from a layout, so they
carry no resource ids and must be reached by text:

- the **type picker** on the create-schedule form (`CreateScheduleActivity:249-276`)
- the **type manager**'s rows
- the Map module's header manager, for comparison — the same pattern, found the same way

A locator written against an id here will not fail loudly; it will return nothing and the
case will skip with a message that sounds like the feature is missing.

### The type list is fetched, not instant

`typeList` is made VISIBLE the moment the picker opens, but its rows arrive afterwards.
Reading it straight after the tap returns an empty list and the case skips with "No type
could be chosen" — which reads like the project has no types, when it has five.

## Automation plan

Six suites, in this order, because each one needs what the one before it makes:

| Suite | Cases | Why here |
|---|---|---|
| `BoxShellTest` | BS- | Read-only. Proves the module opens before anything else is blamed |
| `BoxTypeTest` | TY- | A type is needed before a schedule |
| `BoxScheduleCrudTest` | SD- | Makes the days everything else hangs off |
| `BoxConflictTest` | CF- | Needs two types and an existing schedule |
| `BoxEventNoteTest` | EV-, NT- | Needs schedule days to link to |
| `BoxViewsTest` | FL-, DD-, PD-, HS- | Needs content to filter, open, print and find in history |

Carried over from the Map module, where each was learned the hard way:

- Every suite creates its own fixtures and deletes them; all named `ZL AUTO`.
- Suites stay **small**. The long ones lose the device before they finish, and a case that
  never completes proves nothing.
- `@ExtendWith(ReasonLogger.class)` on every suite, or a skip says nothing about why.
- Diagnostics must be **cheap**: a failure message that scrolls the UI to describe itself
  changes the thing it is describing.
- Wire each suite through all six points in `ADDING_A_MODULE.md` — including the `.run/`
  entry, which is the only part the user actually sees.
