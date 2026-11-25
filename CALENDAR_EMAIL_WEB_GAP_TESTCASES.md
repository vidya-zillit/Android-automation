# Calendar + Email — web gap cases (Android)

Every MISSING / partial row of `web-e2e/gaps/calendar.md` and `gaps/email.md`, with where it is
automated and its last result, or why it is not automatable. App source checked:
`Zillit-Android-devbuild` (build Zillit_32539-dev). Runs: 2026-11-23/24 on emulator-5554/5558
(Vidya Testing) and the Pixel (Vidya Pixel, invitee).

Legend: **PASS** · **FAIL (bug)** = fails on a real app defect named in the assertion ·
**SKIP** = precondition missing, reason logged · **NOT AUTOMATABLE** = reason given.

## Calendar

### CalendarRecurringScopeTest — `TestOrder.CALENDAR_RECURRING_SCOPE` (200)

| Case | Web id | Checks | Last result |
|---|---|---|---|
| editSingleOccurrence | #66 | "This event" edit hides Repeat; only that day renamed | PASS |
| editThisAndFollowing | #67 | "This and following events (from …)" splits the series | PASS |
| deleteSingleOccurrence | #70 | Scope dialog Cancel deletes nothing; "This event" cancels one day | PASS |
| deleteThisAndFollowing | #71 | Days 4-5 cancelled, day 2 live | PASS |
| customWeekdays | #68 | Custom Mon+Wed on Wednesday, not Tuesday | PASS |

### CalendarWebGapTest — `TestOrder.CALENDAR_WEB_GAP` (201)

| Case | Web id | Checks | Last result |
|---|---|---|---|
| duration65Minutes | #42 | 12:00-1:05 PM detail duration | **FAIL (bug)**: "Duration: 1.5 hours" — EventDetailBottomSheet.kt:300-301 prints `${hours}.${mins}` |
| timezoneKeepsInstantAndCardMatchesDetail | #30, #105, #31 | NY converts the form, detail keeps device time; Created card = detail | #30 PASS (form 2:00 PM → 3:30 AM NY; detail still 2:00-3:00 PM). #31 **FAIL (bug)**: card "Fri, Nov 27 3:30 AM - 4:30 AM" vs detail "2:00 PM - 3:00 PM" — card formats in event.timezone (EventCardAdapter.kt:268), detail in device zone. #105 NOT AUTOMATABLE: the picker's timezone list (from the API) has no Kathmandu entry |
| personalDetailContent | #26 (+CAL-09) | trimmed title, Personal, "Duration: 1 hours", week, no member-only rows, no reminder row | PASS |
| whitespaceTitleRefused | #46, #99 | "   " refused, 3-char title saves | PASS |
| endUnder30MinutesPushed | #49, #102 | End 10:10 → 10:30 (Android minimum 30 min, web 15) | PASS |
| pastStartTodayMovedForward | #54, #21 | Start 12:00 AM today → now rounded to 5 min, End +1 h | PASS (skips with a reason between 22:50 and 00:30) |
| meetInPersonCallNeedsLocation | #48 | "& Call" without location refused; "Meet in Person" saves | PASS |
| endDateRolls | #103, #104, #106 | End Date rolls for overnight, incl. Dec 31 → Jan 1 | PASS |
| overnightShowsOnBothDays | #29 | 11 PM → 12:30 AM on both days | **FAIL (Android gap)**: day 2 empty — agenda groups by start date (CalendarViewModel.kt:86-97) |
| editMovesDateAndTime | #58, #63 | Cancel in delete confirm keeps it; edit hides type, "Update Events", moves date+time | #63 PASS. #58 **intermittent**: twice (≈00:40 and ≈01:20 IST, 2026-11-24) the edit form showed Nov 27 1:00-2:00 PM but the saved event read 12:30-1:30 AM; at 03:56 IST it saved correctly (PASS). Not a confirmed bug — the check stays and names it if it recurs |
| reminderAndColourSaved | #32 | "30 minutes before" + Warm Blue #5B7FA5, reminder in detail | PASS |
| customRepeatNeedsWeekday | #51 | Custom with no weekday refused | PASS (refused by the server; the client has no selectedDays check) |
| pastReminderRefused | #50 | all-day today + "5 minutes before" | **FAIL (bug)**: saves — no reminder-in-the-past check |
| pastDateFromGridRefused | #20 | past grid day + FAB | **FAIL (bug)**: an all-day event on yesterday saves (CalendarFragment.kt:323-336 + no past-date rule) |
| expiredEventReadOnly | #25, #45 (CAL-17) | "This event has expired", no Edit/Delete | PASS |
| eventsMenuAndLists | #4, #5, #72, #73, #74, #77, #79, #80, #83 | Events menu; Received/Created/Declined tabs; no-match empty states; reopen resets | PASS |
| joinCallScreen | #81, #82 | "Join Call on Events", "No call events" | PASS |
| viewsPagingAndToday | #16, #15, #17 | Month/Week/Day; Day ±1, Today, TODAY badge; Week paging | PASS |
| fabPrefillsSelectedDay | #27 | tapped day pre-fills Start Date | PASS |
| callTypeOptionsAndCallLine | #35 | exactly 4 call types; Call Line rule; Personal hides People | PASS |
| recurrenceDefaults | #64, #65, #112 | Repeat/Reminder lists; Repeat Until defaults; chips only for Custom | PASS |
| textEdgeCases | #98, #100, #101 | unicode/RTL/HTML title+description, 150-char title | PASS |
| timezoneNoMatch | #113 | "No timezones match your search" | PASS |
| organizerExcludedEvent | #34 case B | card "- Excluded" tag + detail "- Excluded" | PASS |
| externalGuestOnlyMeetInPerson | #34 case C | guest-only Meet in Person: no Call Line, no Join Call, "External Guests" section | PASS |
| externalGuestInput | #38 | invalid not added, IME Done adds, duplicate refused, remove, form Clear | PASS |
| inviteePickerModes | #39, #115 | admin tabs, Select All/Deselect All, department pick, Clear | **FAIL (bug)** (05:48): tabs, Select All count ("Done (1)"), second-tap deselect, department pick ("1 person invited") and form Clear all PASS; the card label after Select All still reads "Select All (1)" instead of "Deselect All". Cause in source: after Select All the VM calls setTab(0), which re-emits an EQUAL user list and the same tab into StateFlows that drop equal values, so AllDepartmentsAdapter is never rebuilt (InviteePickerFragment.kt:156-178, 425-427; InviteePickerViewModel.kt:274-278, 330-334) |
| line2KeptAndPickerHidesCreator | #36, #59 | Line 2 reopens as Line 2; edit picker hides the creator (search control finds Vidya Pixel) | PASS (04:05, with the Vidya Pixel search control) |
| createdCardEditDelete | #61, #62 | card Edit saves description; card Delete removes the card; grid detail "This event has been cancelled" | PASS (04:05) |
| listTodayAndDateFilter | #75 | Today checkbox + Select date; button shows the date | PASS |
| createdListPaginates | #78 | 32 cards after scrolling (page 20) | PASS |
| startDatePickerGreysPast | #53 | yesterday disabled in the Start Date picker | PASS |
| viewModeSurvivesUnitSwitch | #12 | Week survives Bulletin → Calendar | PASS |
| doubleTapCreateMakesOne | #116, CAL-32 | two Create taps → one event | PASS |
| locationPicker | #40 | map picker sets a location; detail shows it | PASS (04:05; emulator has no GPS, location "0.00000, 0.00000") |

### CalendarInviteTest — `TestOrder.CALENDAR_INVITE` (202), two devices

`ZILLIT_UDID=<emulator> ZILLIT_RECEIVER_UDID=34281FDH2005XC ./run-and-report.sh --only CalendarInviteTest`
(emulator = creator Vidya Testing, Pixel = invitee Vidya Pixel; only those two accounts are invited).

| Case | Web id | Checks | Last result |
|---|---|---|---|
| creatorCreatesMembersEvent | #34 A, #10, #37 | Line 1, "1 person invited"; Members / Audio Call / Join Call / "1 Invitee" / "Pending 1" | PASS (04:54) |
| inviteeAccepts | #11, #90, #96 | "Pending invitation", Accept+Decline, admin Edit; Accept → "You accepted this event" | PASS (04:54) |
| inviteeDeclinesWithReason | #91, #133, CAL-29, CAL-34 | decline with reason; Declined → Received; "You declined this event"; no Join Call | PASS (04:54) |
| creatorSeesDecliner | #92, CAL-06 | Show decliners (1), name + quoted reason, no Request Reason | PASS (04:54) |
| scopedDeclineOnRecurringInvite | #94, #134 | "This and following" decline from day 2; day 1 pending | PASS (04:54) |
| requestReasonNeedsText | #92 | Send Request disabled for empty/blank (never sent) | PASS (04:54) |
| badgeGoesUpWithInvite | #137, CAL-21 | invitee Events badge goes up | PASS (04:54) |
| cardAcceptLeavesPending | #93, #137 | card Accept leaves Pending, lands in Accepted, badge down | PASS (04:54) |
| organizerEditReachesInvitee | #95 | invitee sees the organizer's rename | PASS (04:54) |
| cancelWhileInviteeDetailOpen | #135 | open invitee sheet after organizer cancels | **FAIL (bug, as on web)** (04:54, reproduced twice): the invitee's open sheet still shows "Pending invitation" + Accept/Decline after the organizer cancelled — the sheet is not refreshed |

### CalendarTest — tightened partial rows and fixes

| Case | Web id | Change | Last result |
|---|---|---|---|
| calendarLoads | #1 | asserts current month title + "Month" (after Today) | PASS |
| monthNavigation | #3 | Today returns to the current month | PASS |
| weekViewToggle | #2 | Week AND Day applied (label), back to Month | PASS |
| createPersonalEvent | #7 | event appears on the agenda | PASS (05:11) — the 04:05 failure was a **flaky test**, not the app: the event saved, but `agendaHasEvent` only sees on-screen rows and today's agenda had grown past the screen. All CalendarTest agenda checks now scroll (`waitAgendaRow`) |
| editEvent | #9 | title + description saved | PASS (05:11) |
| colorPicker | #33 | preset swatch (was always SKIP: GridView cells are FrameLayouts) + "Select custom color" dialog | PASS |
| backReturnsToCalendar | #41 | reopened form has an empty title | PASS |
| viewEventDetail | #43 | sheet really closed after × | PASS |
| deleteEvent | #62 | cancelled detail: status text, "Cancelled:" title, no Edit | PASS |
| updateRecurringAll | #69 | All-events edit keeps Repeat (Daily) and changes to Weekly; tomorrow empty | PASS |
| deleteRecurringAll | X1 | "Cancelled:" row found by scrolling the agenda | PASS (04:05) |
| allDayHidesTimePickers | #110 | "Does not repeat" hides Repeat Until | PASS |
| personalTypeNoInvitees | minor | AND instead of OR (was un-failable); control scrolled into view | PASS |
| quickActionsPresent | minor | Events AND Join Call | PASS |
| timezonePicker | (failure 2026-11-23) | test bug fixed: `hideKeyboard()` = BACK closed the picker; now asserts London applied | PASS |
| reminderPicker | (always SKIP) | label "10 minutes before" | PASS |
| createRecurringEvent | — | agenda row found by scrolling (today's list is longer than the screen) | PASS |

### EmailCalendarTest

| Case | Web id | Last result |
|---|---|---|
| emailCalendarEventOnHomeCalendar | #132 | PASS |

### Calendar rows NOT AUTOMATABLE

| Web id | Gap | Reason |
|---|---|---|
| #97, #138 | non-admin: no Edit/Delete on others' events, no All Departments tab | both project members (Vidya Testing, Vidya Pixel) are admins; demoting a real user is not allowed without a safe restore |
| #139, #140 | no Calendar view / post right | same: changing real users' tool rights |
| #59 (2nd half) | add another user on edit | the project has only two members and both are already used (creator + Vidya Pixel) |
| #44 | Box Schedule event has no Edit/Delete | needs a box-schedule-mirrored calendar event fixture (created by the Box Schedule module); none in the project calendar |
| #55, #56, #57, #60, #118-#126 (M21) | offline / HTTP 500 error states | network faults on shared devices are not allowed; HTTP 500 cannot be injected through Appium |
| #84 | Join on a Line 1 event opens the lobby | starts a real call in the shared project (rings the other member) |
| #136 | live un-invite | removing the only invitee needs a replacement guest; the only allowed addresses belong to the two members |
| #127, #128 | dark/light theming | needs `cmd uimode night` — a device-wide system setting on shared devices |
| #40 (last step) | tapping the location opens Google Maps | leaves the app for an external app; the in-app part is automated |
| WEB-ONLY (#13, #14, #18, #19, #22, #23, #24, #28, #76, #85, #86, #87, #89, #108, #129, #130, #131) | — | no Android equivalent (see gap file) |

## Email

| Case | Suite | Web id | Last result |
|---|---|---|---|
| folderDrawer (tightened) | EmailTest | W1, W2 | PASS — Inbox/Sent/Drafts/Trash all required; nav_mail/calendar/contacts/settings present |
| searchNoMatchShowsEmpty | EmailTest | W6 | PASS |
| composeHeaderStaysWhileTyping | EmailTest | EMAIL-03 | PASS |
| drawerCalendar (tightened) | EmailDrawerTest | W3 | PASS — new calendar FAB required; back returns to the list |
| draftSaveReopenDelete | EmailExtrasTest | W10 | PASS (04:58). The earlier failures were test-side: a reopened draft shows To as a collapsed chip and hides et_to, so `isLoaded()` never held; it now checks subject + Send |
| selfMailArrivesUnread | EmailExtrasTest | EMAIL-02, W8 | **FAIL (bug — EMAIL-02 reproduces, conditionally)**: at 05:01 and 05:50 the self-sent mail arrived in Inbox WITHOUT the unread dot (Inbox count 2-3), with Conversation view ON (recorded at 05:50); at 01:10 and 03:58 the dot was shown (Conversation-view state not recorded then). Likely: in conversation view the new Inbox copy is grouped with the already-read Sent copy, so the thread shows as read. Unread-filter step not reached |
| ruleReadbackShowsFullSentence | EmailExtrasTest | EMAIL-01 | PASS — full sentence, no truncation (list-card 2-line ellipsis left as a product decision) |
| sendRequiresRecipient | EmailTest | W9 (conflict) | PASS — Android disables Send; correct per source |

### Triage of the 4 EmailTest failures from 2026-11-23 (all test-side, now PASS)

1. **richTextFormatting — "btn_clear_format should be tappable"**: test outdated. The format toolbar is a
   HorizontalScrollView wider than the screen; the last buttons start off-screen and are not in the
   accessibility tree. `ComposeEmailPage.tapFormat` now scrolls the strip.
2. **emptySubjectConfirm — "No subject dialog"** and 3. **composeAndSend — "compose should close"**:
   not an app bug. The failure screenshot shows a heads-up notification ("New message received …
   Vidya Pixel says …", from other suites' chat traffic) drawn over the toolbar, so the Send tap
   landed on the banner. The app's no-subject dialog and send/close are intact
   (ComposeActivity.kt:883-895, 961-972, 1229-1231). `tapSend()` now verifies the tap took effect
   (dialog / progress / compose closed) and retries after the banner times out.
4. **sentEmailInSentFolder**: consequence of 3.

Also fixed: `EmailPage.isLoaded` counted the email DETAIL screen as the list (it reuses `rv_emails`),
so recovery stopped on the detail and later cases skipped (searchEmails, emailSettings) — both PASS now.

### CalendarTest timezone failure from 2026-11-23

`timezonePicker` — test bug, not app: `selectTimezone` called `hideKeyboard()`, which is a BACK press on
UiAutomator2; with no IME up it closed the picker Activity, the row wait timed out and the fallback BACK
closed the form, so tvTimezone read "". Fixed in `CalendarPage.EventFormView.selectTimezone`.

## Leftover test data (2026-11-23/24)

- Vidya Pixel's calendar: series "ZL_AUTO Cal RecA <run>" (run killed on the Pixel before its sweep).
- Vidya Testing: "QA Recur 869647 EDIT" (CalendarTest round 1, deleteRecurringAll skipped), the past
  all-day "ZL_AUTO Cal Past <run>" events (#20 bug — expired, the app cannot cancel them), and the
  organizer-excluded "ZL_AUTO Cal Excl <run>" events (not on the organizer's grid, so the sweep cannot
  reach them). Drafts: "ZL_AUTO draft 730613" and several "(No subject)" drafts from older EmailTest runs.
