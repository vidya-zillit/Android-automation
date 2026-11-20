# Production Diary / Box Schedule — screens, ids and flows

Read from the app source, not from a device, the same way as `MAP_IDS_AND_ROUTES.md`.

**One module, two names.** `strings.xml` calls it *Production Diary/Box Schedule*
(`bs_title`). The live code is `bottomNav/tools/new_boxschedule`, reached from Tools as
`BoxScheduleActivity`. The older `bottomNav/tools/boxschedule` belongs to **pre-production**
and is a different feature — do not automate it by mistake.

**Activities, not a nav graph.** Unlike Map, this module is a set of Activities started with
Intents, so there is no `*_nav_graph.xml` to read routes from. The flow below is taken from
the click handlers.

## Flows confirmed with the project owner

Walked through by the person who owns this module, then each point checked against the
code. Everything below was confirmed in both places.

| Behaviour | Where the code says so |
|---|---|
| Calendar view has **Month / Week / Day** | `CalendarFragment.calendarMode` |
| List view has **By Date / By Schedule** | `ListFragment.listMode` |
| **Set as Default** remembers Calendar or List | `SharedPref.setBoxScheduleDefaultView()` |
| A new **type** can be added from the create-schedule page | `TypeManagerActivity`, plus an inline `newTypeForm` |
| Dates can be set three ways: **range / calendar / day-wise** | `dateTabMode` |
| An **event links to a schedule day, optionally** — it is a reminder, not ownership | "Only attach scheduleDayId when the user explicitly linked one" |
| **Notes are general or personal**; general ones are distributed | `noteType`, server-supplied options |
| **PDF asks whether to include personal notes** | `includePersonalNotes`, on Share / Print / Distribute-to-DD alike |
| Filters cover **schedules / events / notes** and **type** | `ListFiltersBottomSheet.Filters` |
| History filters by **date and action** | ALL · Added · Changed · Removed · Shared · Printed |
| A day opens with **edit and delete** per row | "schedules with Edit/Delete, events with Edit/Remove, notes with Delete" |

### Creating an event asks about the Home Calendar

On save, create mode shows a prompt that cannot be dismissed by tapping away:

> *"Would you like to set a reminder for this event on your Home Calendar?"* — Yes / No

The answer becomes `createEventInCalendar` on the wire, and a Yes makes the event appear in
the Calendar module too. Two details that matter for testing:

- **Create mode only.** Editing an event reuses the flag it already had — the code says this
  is deliberate, "to avoid accidental flips on minor edits". So an edit must NOT re-ask.
- **`setCancelable(false)`.** The choice is required; there is no way past it except Yes or
  No. A test that expects to tap outside and continue will hang.

The bridge the other way is one-way only: `CalendarEventMapper` maps calendar events INTO
box-schedule surfaces for display, and its own comment says "Calendar remains the source".

### Known defects to cover, both already admitted in the code

1. **ZL-19566 — Replace over scattered dates.** Every date mode now sends `by_days`, because
   `by_dates` lets the server fill the whole span: picking Jun 17 / 26 / 29 and choosing
   Replace used to wipe all thirteen days in between. Web and iOS always sent `by_days`;
   this was Android-only.
2. **The linked schedule day does not show when editing an event.** `syncLinkDaySpinnerSelection()`
   exists but is driven from two places, and its own comment admits "whichever lands last
   wins" — a race. The owner reports the day is simply missing on edit.

## Screens

### `BoxScheduleActivity`

Tools → Production Diary/Box Schedule. Calendar/List views + the drawer.

Layout: `new_box_activity_box_schedule.xml` — 29 ids

```
  btnCalendarView  btnCloseDrawer  btnListView  btnMenu  btnSetDefaultView
  drawerCreateEvent  drawerCreateNote  drawerCreateSchedule  drawerDistributeToDd  drawerEditTypes
  drawerHistory  drawerHistoryBadge  drawerLayout  drawerPresets  drawerPrint
  drawerShare  drawerThemeIcon  drawerThemeLabel  drawerThemeSwitch  drawerThemeToggle
  fragmentContainer  legendContainer  loadingOverlay  menuBadgeCount  navDrawer
  refreshingIndicator  toolbar  tvPrepared  viewToggle
```

### `CreateScheduleActivity`

Create or edit a schedule DAY (type + dates). Conflict lives here.

Layout: `new_box_activity_create_schedule.xml` — 41 ids

```
  btnAddNewType  btnAddPickedDate  btnByDays  btnByEndDate  btnCalNext
  btnCalPrev  btnCancelNewType  btnChangeType  btnCreateType  btnMinus
  btnPlus  btnSave  calPickerWeekdays  calendarContent  calendarPickerGrid
  dateModeTabs  dateRangeContent  dayWiseContent  endDateContainer  etNewTypeName
  etNumberOfDays  etTypeSearch  newTypeColorDot  newTypeForm  numberOfDaysContainer
  pickedDatesChips  rangeSubModeToggle  selectedTypeContainer  selectedTypeDot  selectedTypeName
  toolbar  tvCalMonth  tvCalSelectedCount  tvDayWiseCount  tvDayWiseEnd
  tvDayWiseStart  tvEndDate  tvStartDate  tvSummary  typeList
  weekdayChips
```

### `CreateEventActivity`

Create or edit an EVENT or a NOTE (same screen, two tabs).

Layout: `new_box_activity_create_event.xml` — 46 ids

```
  btnAddExternalGuests  btnEventTab  btnLocationChange  btnLocationRemove  btnNoteTab
  btnSave  cbOrganizerExcluded  endDateColumn  etEventDesc  etEventTitle
  etLocation  etNoteText  etNoteTitle  eventColorPicker  eventForm
  eventNoteToggle  layoutCallLine  linkDayCard  locationActionsRow  locationBottomSpacer
  noteColorPicker  noteDistributeSection  noteForm  noteTypeToggle  repeatEndDateContainer
  repeatRowContainer  spinnerCallLine  spinnerCallType  spinnerLinkDay  spinnerReminder
  spinnerRepeat  spinnerTimezone  switchFullDay  tabContainer  textColorPicker
  timeRow  toolbar  tvDistributeTo  tvDistributeToNote  tvEndTime
  tvEventEndDate  tvEventStartDate  tvExternalGuestsCount  tvNoteDate  tvRepeatEndDate
  tvStartTime
```

### `DayDetailActivity`

One day's contents.

Layout: `new_box_activity_day_detail.xml` — 18 ids

```
  actionButtons  addToThisDaySection  btnAddEvent  btnAddNote  btnAddSchedule
  dayNumberCircle  emptyState  eventsContainer  notesContainer  pastBadge
  schedulePillsContainer  todayBadge  toolbar  tvDayName  tvDayNumber
  tvEventsLabel  tvMonthYear  tvNotesLabel
```

### `HistoryActivity`

Revisions + activity log.

Layout: `new_box_activity_history.xml` — 7 ids

```
  btnClearDate  btnPickDate  emptyState  historyList  historyLoader
  spinnerFilter  toolbar
```

### `PresetListActivity`

Distribution presets.

Layout: `new_box_activity_preset_list.xml` — 7 ids

```
  etSearch  fabCreate  loader  rvPresets  searchRow
  toolbar  tvEmpty
```

### `CreatePresetActivity`

Create/edit a preset.

Layout: `new_box_activity_create_preset.xml` — 11 ids

```
  btnSave  btnSelectAll  etPresetName  etSearchUsers  rvUsers
  saveLoader  selectedChips  selectedChipsScroll  toolbar  tvUsersEmpty
  tvUsersHeader
```

### `SelectInviteesActivity`

Who a schedule/event is distributed to.

Layout: `new_box_activity_select_invitees.xml` — 34 ids

```
  bodyAllDepartments  bodyDepartments  bodyPresets  bodySelf  bodyUsers
  btnCancel  btnDone  btnSelectAllDepartments  btnSelectAllInDepartments  btnSelectAllInUsers
  btnSelectSelf  btnTabScrollLeft  btnTabScrollRight  etSearchDepartments  etSearchPresets
  etSearchUsers  presetLoader  rvDepartments  rvPresets  rvUsers
  selectedChips  selectedChipsDivider  selectedChipsScroll  tabAllDepartments  tabDepartments
  tabPresets  tabScroll  tabSelf  tabUsers  toolbar
  tvAllDepartmentsTitle  tvDepartmentsEmpty  tvPresetsEmpty  tvUsersEmpty
```

### `ConflictDialog`

Replace / Extend / Overlap, after a 409.

Layout: `new_box_dialog_conflict.xml` — 5 ids

```
  btnBack  optionExtend  optionOverlap  optionReplace  tvConflictMessage
```

### `DayDetailBottomSheet`

Day contents as a sheet.

Layout: `new_box_bottomsheet_day_detail.xml` — 10 ids

```
  btnAddEvent  btnAddSchedule  emptyState  eventsContainer  notesContainer
  pillsContainer  tvDayName  tvEventsLabel  tvFullDate  tvNotesLabel
```

### `ViewEventBottomSheet`

One event, read-only.

Layout: `new_box_bottomsheet_view_event.xml` — 8 ids

```
  btnVeClose  tvVeDate  tvVeHeaderTitle  tvVeTime  tvVeTitle
  veDetailContainer  veHeader  veJoinCall
```

### `HistoryBottomSheet`

History as a sheet.

Layout: `new_box_bottomsheet_history.xml` — 6 ids

```
  btnPickDate  emptyState  historyList  spinnerFilter  tvEmptyDesc
  tvEmptyTitle
```

### `DeleteRecurringDialog`

This occurrence / all occurrences.

Layout: `new_box_dialog_delete_recurring.xml` — 15 ids

```
  btnDeleteRecurringCancel  btnDeleteRecurringClose  btnDeleteRecurringConfirm  dotDeleteEventColor  optDeleteAll
  optDeleteFollowing  optDeleteSingle  rbDeleteAll  rbDeleteFollowing  rbDeleteSingle
  tvDeleteEventDateTime  tvDeleteEventRecurrence  tvDeleteEventTitle  tvDeleteFollowingDesc  tvDeleteSingleDesc
```

### `PdfOptionsDialog`

Calendar vs List layout, personal notes on/off.

Layout: `new_box_dialog_pdf_options.xml` — 8 ids

```
  btnPdfOptionsCancel  btnPdfOptionsConfirm  rbPdfLayoutCalendar  rbPdfLayoutList  rbPdfNotesWith
  rbPdfNotesWithout  rgPdfLayout  rgPdfNotes
```

### `ScheduleScopeDialog`

Which days an edit applies to.

Layout: `new_box_dialog_schedule_scope.xml` — 16 ids

```
  btnScheduleScopeCancel  btnScheduleScopeClose  btnScheduleScopeConfirm  dotScheduleScopeColor  ivScheduleScopeIcon
  optScopeComplete  optScopeThisDate  rbScopeComplete  rbScopeThisDate  tvScheduleScopeChoose
  tvScheduleScopeMeta  tvScheduleScopeName  tvScheduleScopeSubtitle  tvScheduleScopeTitle  tvScopeCompleteDesc
  tvScopeThisDateDesc
```

### `ShareDialog`

Share the generated PDF.

Layout: `new_box_dialog_share.xml` — 6 ids

```
  btnClose  btnCopyLink  btnCopyText  btnGenerateLink  linkContainer
  tvShareLink
```

### `TypeManagerActivity`

Schedule types — add / edit / delete.

Layout: `new_box_dialog_type_manager.xml` — 7 ids

```
  btnAddType  btnDone  etNewTypeName  newTypeColor  toolbar
  tvTypeError  typesList
```
