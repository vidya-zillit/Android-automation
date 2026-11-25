# Calendar: web E2E vs Android Appium gap analysis

Web source: `web-e2e/calendar.md` (140 tests, 36 sheet bugs).
Android suite: `src/test/java/com/zillit/automation/tests/calendar/CalendarTest.java` (29 tests) + `pages/calendar/CalendarPage.java`; also `tests/email/EmailCalendarTest.java`.
App source: `Zillit-Android/app/src/main`. Path shorthands used below:
- `NC/` = `java/com/zillit/zillitapp/bottomNav/new_calendar/`
- `LAY/` = `res/layout/`
- `STR` = `res/values/strings.xml`

Web test numbers (#N) follow the order of the web file (1 = "Calendar tab renders the CalendarV3 shell", 140 = "without the Calendar post right B gets no create button").

## Summary

| Class | Web tests | Notes |
|---|---|---|
| COVERED | 15 | Mostly partial: the Android test checks less than the web case (gaps noted per row) |
| MISSING | 105 | Grouped into 23 proposed Android cases/case-groups below |
| CONFLICT | 3 | 2 in CalendarTest, 1 in EmailCalendarTest |
| WEB-ONLY | 17 | Browser/DOM/route/drag features absent on Android |
| **Total** | **140** | |

How Android differs from web (important for writing cases):
- Week view is a one-row 7-day strip + agenda list, not an hourly grid (`NC/ui/calendar/CalendarViewModel.kt:66-84`). Day view is an hourly timeline (`HourlyTimelineAdapter`). Tapping a grid cell only selects the day; the form opens only from the FAB, prefilled with the selected day (`NC/ui/calendar/CalendarFragment.kt:323-336`).
- Minimum event length is 30 min (web 15): `NC/domain/util/EventFormLogic.kt:16`. Out-of-range End or past Start times are auto-corrected with a toast, not refused (`NC/ui/form/EventFormViewModel.kt:241-284`).
- Title rule: one message for empty and short titles, "Title must be at least 3 characters" (`EventFormLogic.kt:75`); web shows "Title is required" for an empty title.
- No recurrence notice dialog, no "Number of ..." count field, and Repeat Until cannot be cleared. It is auto-filled when a frequency is picked (`EventFormLogic.kt:272-281`, `EventFormViewModel.kt:318-321`).
- Errors are Toasts (not readable to UiAutomator2 on Android 12+). Assert state instead: the form stays open, a field changes, or a row appears.
- Validation/RSVP strings to assert: `STR:5519-5522` ("Pending invitation", "You accepted this event", "You declined this event", "This event has expired"), `STR:5529-5534` (Accept/Decline Invitation dialog), `STR:5570` ("Repeat end date can't be before the start date"), `STR:685` (edit button reads "Update Events").

---

## MISSING (prioritised)

### M1. Members event with invitee, call and detail (web #34, #10, #37) — P1
Feature: `LAY/new_calendar_fragment_event_form.xml` (btnTypeMembers, tvCallType, tvCallLine, btnAddInvitees, tvInviteeCount, btnAddExternalEmails, cbExcludeMe); detail `NC/ui/detail/EventDetailBottomSheet.kt:354-440`.
- **Case A: "Create a Members Audio Call event with one invitee; detail shows Members, Audio Call, Join Call, See invitees"**
  Steps: FAB → btnTypeMembers → title `QA Members <ts>` → tvDate a future day → tvCallType "Audio Call" → check tvCallLine = "Line 1" → btnAddInvitees → search etSearch the second test account → tap row → btnDone → check tvInviteeCount = "1 person invited" (`EventFormFragment.kt:517-520`) → btnSave.
  Expected: form closes; the event is in the agenda for that day. Detail: tvEventType "Members", tvCallType "Audio Call", btnJoinCall visible (`EventDetailBottomSheet.kt:388-394`), layoutBottomBar gone for the creator (`:474-477`), tvInviteeSummaryCount "1 Invitee". Tapping layoutInviteeSummary opens the Invitees dialog (`LAY/new_calendar_dialog_invitee_list.xml`) with tvPendingCount "Pending 1" (`NC/ui/dialogs/InviteeListDialog.kt:158`). Clean up by deleting.
- **Case B: "Organizer-excluded Members event"**: same as A, tick cbExcludeMe ("The organizer will not be a part of this event", `STR:5500`). Expected: saves; the detail shows tvCreatorExcluded "- Excluded" (`EventDetailBottomSheet.kt:386`); the event is listed in Events → Created Events → Invites Sent.
- **Case C: "External-guest-only Meet in Person event"**: Members → tvCallType "Meet in Person" → check layoutCallLine is gone (`EventFormLogic.kt:55`, `EventFormFragment.kt:548`) → btnAddExternalEmails → etEmail `qa+<ts>@example.com` → btnAddEmail → answer the add-as-external prompt (`STR:5057`) → btnDone → check tvExternalEmailCount "1 external guest" → save. Expected: in the detail, btnJoinCall is gone (meet_in_person is not joinable) and the Invitees dialog shows the "External Guests" section with the email.

### M2. Recurring scope: edit or delete one occurrence, this-and-following, all (web #66, #67, #68, #70, #71) — P1
Feature: `NC/ui/dialogs/RecurringActionDialog.kt` + `LAY/new_calendar_dialog_recurring_action.xml` (rbSingle "This event", rbThisAndFuture "This and following events (from <date>)" `RecurringActionDialog.kt:58`, rbAll "All events", btnOk "Continue", btnCancel). The form hides the Repeat section for single/thisAndFuture (`EventFormFragment.kt:479-484`). CalendarPage already has `pickRecurringScope("single"|"future"|"all")`.
- **"Edit this event only renames just that occurrence"**: create a Personal Daily series (start D+2, Repeat Until D+5). Day view D+3 → open → btnEdit → rbSingle → btnOk → check layoutRecurrenceSection is gone → title += " ONE" → save. Expected: D+3 shows "... ONE"; D+4 keeps the original. Events → Created Events → Personal → the card's btnToggleOccurrences "View Full Event Details" shows tvEditedLabel "Edited occurrence" (`LAY/new_calendar_item_occurrence.xml`).
- **"Edit this and following splits the series"**: D+4 → rbThisAndFuture → " LATER". Expected: D+3 has the original title; D+5 has "... LATER".
- **"Delete this event only; Cancel deletes nothing"**: btnDelete → the scope dialog shows rbSingle → btnCancel → the event is still live. Then rbSingle → confirm. Expected: only that day shows "Cancelled: <title>" (`NC/ui/calendar/DayAgendaAdapter.kt:38-41`); the next day is live.
- **"Delete this and following"**: from D+4. Expected: D+4 and D+5 cancelled, D+3 live.
- **"Custom Mon/Wed series lands on those weekdays only"**: tvRecurrence "Custom" → chipGroupDays shown (`EventFormFragment.kt:488`) → tick Mon, Wed → save. Expected: in Day view the event is on Mon and Wed but not on Tue.

### M3. 65-minute duration reads "1.5 hours" (web #42) — P1, will catch a live Android bug
`NC/ui/detail/EventDetailBottomSheet.kt:296-303` builds `"Duration: ${hours}.${mins} hours"`, so 65 min → "Duration: 1.5 hours" and 90 min → "1.30 hours".
- **"Detail duration of a 65-minute event is not '1.5 hours'"**: Personal event D+2, 12:00 PM → 1:05 PM (the End picker allows it: 65 ≥ 30). Open the detail and read tvDuration. Expected: not "Duration: 1.5 hours" (for example "1 hour 5 minutes"). Currently fails.

### M4. Created/Received card time vs detail time with a non-device timezone (web #31) — P1, will catch a live Android bug
Cards format in `event.timezone` (`NC/ui/common/EventCardAdapter.kt:268`); the detail formats in the device zone (`EventDetailBottomSheet.kt:289-290`). This is the same defect as on web.
- **"Card time equals detail time for an event saved in New York"**: Personal, D+3, 2:00–3:00 PM device time → tvTimezone → etSearch "New York" → tvTimezoneValue → save → Events → Created Events → Personal tab → etSearch title. Expected: tvCardDateTime shows the same range as the detail's tvTimeRange.

### M5. RSVP and invitee responses (web #11, #90, #91, #92, #93, #94, #133, #134) — P1, needs a second account
Feature: detail bottom bar `EventDetailBottomSheet.kt:443-500`; `NC/ui/dialogs/AcceptRejectDialog.kt` (tvDialogTitle "Accept Invitation"/"Decline Invitation", rgScope "APPLY TO" for recurring, etReason "Let the organizer know...", btnSubmit); card buttons btnCardAccept/btnCardReject (`LAY/new_calendar_item_event_card.xml`). Web invites "Lava Device", which looks like the Android test device account, so a web/iOS run or an API seed can create the invites.
- **"Pending invite shows Accept/Decline; accept marks it accepted"** (#11, #90): open a seeded invite → tvInvitationStatus "Pending invitation", btnAccept and btnReject shown, no btnEdit → btnAccept → dialog "Are you sure you want to accept this invitation?" → btnSubmit. Expected: on reopen, "You accepted this event", btnAccept gone, btnReject shown; listed under Received → Accepted.
- **"Decline with a reason"** (#91, #133): btnReject → etReason `qa reason <ts>` → btnSubmit. Expected: "You declined this event"; the agenda row reads "Declined: <title>" struck through (`DayAgendaAdapter.kt:43-46`; sheet bug CAL-07); btnJoinCall hidden (`:388-392`); listed in Declined Events → Received. Tapping btnAccept again moves it back to Accepted.
- **"Card-level Accept from Received → Pending"** (#93): btnCardAccept → confirm. Expected: the card leaves Pending.
- **"Scoped decline on a recurring invite"** (#94, #134): decline with rbThisAndFuture from the 2nd occurrence. Expected: the 1st is still "Pending invitation" and later ones are declined. A single accept on one day leaves the other days pending.
- **"Organizer sees the decliner and can request a reason"** (#92): Declined Events → Created tab → btnToggleDecliners "Show decliners (1)" → "Request Reason" → btnSendRequest disabled while etMessage is empty or spaces (`NC/ui/dialogs/RequestReasonDialog.kt:141-142`) → btnCancel. Do not send. Also catches sheet bug CAL-06 (decliner row without a name).

### M6. Events menu and the four list screens (web #4, #5, #72, #73, #74, #75, #77, #78, #79, #80, #81, #82, #83) — P2
Feature: btnEvents → popup rowReceived/rowCreated/rowDeclined (`CalendarFragment.kt:507-572`, `LAY/new_calendar_popup_events.xml`); screens are Activities: `LAY/new_calendar_fragment_received_events.xml` (tabs Pending/Accepted/Expired, cbToday, btnDatePicker "Select date", etSearch, emptyState "No events"), `..._created_events.xml` (Invites Sent/Personal/Expired), `..._declined_events.xml` (Received/Created, emptyState "No declined events"), `..._join_call_events.xml` (etSearch, emptyState "No call events").
- **"Events menu lists Received, Created and Declined Events"** (#4): tap btnEvents. Expected: the three rows.
- **"Received Events opens on Pending; Accepted/Expired tabs load; back closes"** (#5, #72, #73). Note: Android keeps the date row on Expired (no visibility toggle in `ReceivedEventsFragment.kt`), unlike web. Assert what the source does and flag the difference to devs.
- **"Search with no match shows the empty state"** (#74, #77, #79, #80): type `zz-qa-no-match` in each tab. Expected: emptyState shown (texts differ from web: "No events" / "No declined events").
- **"Today / Select date filter"** (#75): tap cbToday, then btnDatePicker (date). Expected: the list reloads and btnDatePicker shows the picked date. There is no Clear button on Android (switching tabs resets the filter, `ReceivedEventsFragment.kt:238-243`).
- **"Created → Personal paginates on scroll"** (#78): Android has infinite scroll, no Load More button (`ReceivedEventsFragment.kt:147-156`, `NC/ui/created/CreatedEventsFragment.kt:122`). Scroll to the end; the card count grows if there are >20 (PAGE_SIZE `NC/util/Constants.kt:7`).
- **"Join Call screen lists today's call events with Join"** (#81, #82): btnJoinCall (header "Join Call on Events", `CalendarFragment.kt:308-313`) → search the title of a Members Audio event created for later today. Expected: card with btnJoinCall "Join" (do not tap); no match shows "No call events".
- **"Reopened list screen starts on its first tab with empty search"** (#83): each open is a new Activity. Cheap regression check.

### M7. Form validation the app enforces (web #46, #99, #48, #49, #102, #54, #21, #53) — P2
Add these to the existing ladder; assert the form stays open or the field auto-corrects.
- **"Whitespace-only title is refused; a 3-character title is accepted"** (#46, #99): Personal, title "   " → save → form stays open (`EventFormLogic.kt:75` trims). Then "abc" → save → form closes, event "abc" in agenda.
- **"Meet in Person & Call requires a location"** (#48): Members, external guest added, tvCallType "Meet in Person & Call", no location → save. Expected: form stays open (`EventFormLogic.kt:112-116`, "Location is required for Meet in Person & Call events"). Then check that "Meet in Person" saves without a location.
- **"End time under 30 minutes is pushed to start + 30"** (#49, #102): Personal, D+2, Start 10:00 AM → End 10:10 AM. Expected: tvEndTime becomes 10:30 AM (`EventFormViewModel.kt:272-280`). Product difference to raise: web minimum is 15 min.
- **"A past start time today is moved forward"** (#54, #21): Personal, today, timed → tvStartTime 12:00 AM. Expected: tvStartTime becomes now rounded up to 5 min and tvEndTime = start + 1 h (`EventFormViewModel.kt:241-261`). The save-time guard is `EventFormLogic.kt:80-88`. The web Day-slot entry point does not exist on Android.
- **"Start Date picker greys out past days; Repeat Until before Start is refused"** (#53): open tvDate → yesterday disabled (`EventFormFragment.kt:620`). Then Weekly → tvRecurrenceEnd → pick a day before Start → save → form stays open (`EventFormFragment.kt:179-191`, `STR:5570`). The Repeat Until picker itself is unconstrained by design (`EventFormFragment.kt:678-697`).

### M8. Validation the app does NOT enforce, likely Android bugs (web #20, #50, #51) — P2, expected to fail today
- **"Cannot create an event on a past date via the grid"** (#20): Month view → tap a past day cell → FAB. Android passes the selected day straight into the form (`CalendarFragment.kt:323-336`, `EventFormFragment.kt:136-145`), and validate() has no past-date rule (`EventFormLogic.kt:59-118`). Expected per web: refused or clamped to today. Today an all-day Personal event on a past date reaches the server.
- **"A reminder already in the past is refused"** (#50): Personal, today, Full Day on, tvNotify "5 minutes before" → save. Expected per web: refused. Android has no such check.
- **"Custom repeat needs at least one weekday"** (#51): Personal D+2, Full Day, tvRecurrence "Custom", no chip ticked → save. Expected per web: refused. Android validate() never checks selectedDays.

### M9. Detail read-only states (web #25, #44, #45) — P2
Edit/Delete gate: `EventDetailBottomSheet.kt:240-252`; status bar `:443-470`. Note `CalendarPage.isDetailOpen()` uses btnEdit/btnDelete, so it reports false for these. Detect the sheet via its "Event Details" heading or tvTitle instead.
- **"Expired event opens read-only"** (#25, #45): tap a past event. Expected: tvInvitationStatus "This event has expired"; btnEdit, btnDelete, btnJoinCall gone. Android computes expiry per open (`NC/ui/detail/EventDetailViewModel.kt:248`), so the web "ended while open" bug (CAL-17) should not reproduce.
- **"Box Schedule event has no Edit/Delete"** (#44): open a box-schedule mirrored event. Expected on Android: btnEdit and btnDelete hidden (`:249`, boxScheduleEvent). Android shows no "Box Schedule Event" notice, unlike web.

### M10. Edit flows (web #32, #36, #58, #59, #61, #63) — P2
- **"Edit moves date and time; type cannot change on edit"** (#58): Personal D+2 4–5 PM → btnEdit → check layoutTypeToggle is gone (`EventFormFragment.kt:430`) and btnSave reads "Update Events" (`STR:685`) → tvDate D+3, tvStartTime 1:00 PM → save. Expected: in Day view D+3 at 1:00–2:00 PM; D+2 is empty.
- **"Reminder and preset colour are saved and shown"** (#32): tvNotify "30 minutes before" + colour "Select from preset colors" → swatch "#5B7FA5" (Warm Blue, `NC/util/Constants.kt:15`) → check tvColorHex. Expected: detail tvReminder "30 minutes before" (`EventDetailBottomSheet.kt:359-366`). Also confirms sheet bug CAL-09 does not apply: with None, layoutReminder is hidden.
- **"Line 2 is kept on reopen"** (#36): Members Video Call, tvCallLine "Line 2", external guest → save → btnEdit. Expected: tvCallLine "Line 2" (`EventFormViewModel.kt:179`).
- **"Invitee picker hides the creator; adding an invitee on edit is saved"** (#59): edit a Members event → btnAddInvitees → search your own name → no row (`NC/ui/form/InviteePickerFragment.kt:53-59`). Add another user → save → the detail invitee count goes up.
- **"Edit and delete from the Created Events card"** (#61): Created → Personal → btnCardEdit → change etDescription → save; then btnCardDelete → confirm. Expected: the card title reads "Cancelled: <title>" (`EventCardAdapter.kt:136`) and no edit/delete buttons remain.
- **"Cancel in the delete confirm keeps the event"** (#63): btnDelete → dialog "Are you sure you want to delete this event?" → btnCancel. Expected: no "Cancelled:" row.

### M11. Personal timed event detail content (web #26) — P2
- **"Personal event detail shows type, duration, week and no member-only parts"**: create Personal D+2, 10:00–11:00 AM, title with surrounding spaces. Expected: tvTitle trimmed (`EventFormLogic.kt:245`), tvEventType "Personal", tvDuration "Duration: 1 hours", tvWeek "Week N of the Year" (`STR:5515`); layoutInviteeSummary, btnJoinCall, layoutCallType, layoutDescription and layoutBottomBar gone. Extends createPersonalEvent + viewEventDetail, which assert only creation and that the sheet opened.

### M12. Overnight events and End Date roll (web #29, #103, #104, #106) — P2, includes a likely Android gap
Form: tvEndDate = Start + 1 when End ≤ Start (`EventFormFragment.kt:461-468`); payload rolls the end (`EventFormLogic.kt:165-171`). Grid: the agenda and month dots use the start date only (`NC/ui/calendar/CalendarViewModel.kt:86-97`, `:250-253`), so an overnight event does not show on the second day. Web expects both days.
- **"11 PM → 12:30 AM ends next day and shows on both days"**: Personal D+4 → Start 11:00 PM → End 12:30 AM → tvEndDate = D+5 → save. Expected: on D+4 and D+5 in Day view (D+5 likely fails).
- **"Start 11:55 PM sets End 12:55 AM next day; Start 12:00 AM sets End 1:00 AM same day"** (#104): read tvEndTime and tvEndDate after each tvStartTime pick (`EventFormViewModel.kt:257-260`).
- **"End before Start means next day"** (#103): 10:00 AM → 9:00 AM. Expected: tvEndDate = Start + 1.
- **"Dec 31 11 PM → Jan 1 12:30 AM"** (#106): same assertions across the year boundary.

### M13. Timezone conversion keeps the instant (web #30, #105) — P2
`EventFormViewModel.kt:341-360` converts date and times on a timezone change.
- **"Changing timezone converts fields and keeps the moment"**: Personal D+3 10:00–11:00 AM (device IST) → tvTimezone "New York" → tvStartTime is no longer 10:00 AM → save. Expected: the detail tvTimeRange still shows 10:00 AM - 11:00 AM device time. Variant (#105): Kathmandu shows 10:15 AM, then back to Kolkata restores 10:00 AM. The existing `timezonePicker` only asserts the field is non-empty.

### M14. Permissions and admin rights (web #96, #97, #138, #139, #140) — P2, needs a second account
- **"Admin can edit another user's event"** (#96): `EventDetailBottomSheet.kt:240` (isCreator || isAdmin). Expected: btnEdit shown on an event created by the other account.
- **"Non-admin: no Edit/Delete on others' events, no All Departments tab"** (#97, #138): `InviteePickerFragment.kt:250-270`. Expected: btnEdit and btnDelete hidden, btnAccept shown; the picker tabs are only "Select Department" and "Select Users". Their own Personal event is still editable.
- **"No Calendar view right hides the Calendar unit"** (#139): host-level check on the Home units strip.
- **"No Calendar post right hides the FAB"** (#140): Android never gates `binding.fab` (`CalendarFragment.kt:323`), so this likely fails the same way as web.

### M15. Call type options and Call Line visibility (web #35) — P3
- **"Call Type lists exactly 4 options and drives Call Line"**: tvCallType dialog = Audio Call, Video Call, Meet in Person, Meet in Person & Call (`EventFormFragment.kt:150`). layoutCallLine is visible for Audio, Video and Meet in Person & Call and gone for Meet in Person (`EventFormLogic.kt:55`). btnTypePersonal hides layoutPeopleSection (`EventFormFragment.kt:511-514`). This extends `callTypePicker`.

### M16. Invitee picker modes, Select All, department pick (web #39, #115) — P3
`LAY/new_calendar_fragment_invitee_picker.xml` (tabs All Departments/Select Department/Select Users, etSearch, tvSelectedCount, btnClearSelection, btnDone "Done (N)" `InviteePickerFragment.kt:296`); Select All card `LAY/new_calendar_item_select_all_card.xml` ("Select All (N)" / "Deselect All", `InviteePickerFragment.kt:427`).
- **"Picker offers its tabs; Select All / Deselect All; department pick fills invitees; Clear empties"**: admin account → All Departments → btnSelectAll → btnDone shows "Done (N>0)" → Deselect All → "Done (0)"; Select Department → tap the first department → Done → tvInviteeCount "N people invited" → tvClearInvitees → the count row is gone. This extends `inviteePicker`, which only checks search and Done.

### M17. External guest input (web #38, #114) — P3
`NC/ui/form/ExternalEmailFragment.kt:158-162` (IME action adds), `:229-241` (toasts "Invalid email address", "Email already added"), item has tvEmail + btnRemove only (no inline edit).
- **"Invalid email is not added, duplicate not added twice, remove empties list, IME Done adds, form Clear empties"**: assert by counting rvEmails rows rather than reading toasts. Inline edit (#114) is web-only.

### M18. Recurrence form defaults (web #64, #65, #107, #109, #111) — P3
`EventFormLogic.kt:272-281`; date change re-derives Repeat Until (`EventFormViewModel.kt:231`).
- **"Each frequency fills its default Repeat Until; Custom shows weekday chips"**: Start D+2 → Daily → tvRecurrenceEnd = D+3; Weekly = D+9; Monthly = D+2 + 1 month; Yearly = + 1 year; Custom = + 1 week and chipGroupDays visible. Change Start to D+4 with Weekly → Repeat Until = D+11.
- **"Clamping"** (#107, #111): Start Feb 29 2028 + Yearly → Feb 28 2029; Jan 31 + Monthly → Feb 28/29 (java.time clamps).
- **"Repeat Until = Start + 1 day saves a 2-day series"** (#109): event shows on Start and Start + 1 only.

### M19. Views, navigation and today (web #12, #15, #16, #17, #27) — P3
- **"View selector offers exactly Month, Week, Day"** (#16): btnViewMode popup (`CalendarFragment.kt:172-181`).
- **"Week/Day next and previous move by 7/1 days"** (#15): tvMonthTitle in Week is "d - d MMM yyyy", in Day "EEE, d MMM yyyy" (`CalendarViewModel.kt:44-60`). btnNextMonth → +7 / +1 day; btnToday returns to the current period. `monthNavigation` covers Month only and does not assert Today.
- **"Today is marked"** (#17): Month cell bgCircle today drawable (`NC/ui/calendar/MonthGridAdapter.kt:47-49`); Day view tvDayCardTodayBadge "TODAY" (`CalendarFragment.kt:400`). Week has no current-time line on Android.
- **"Tapping a future day then FAB pre-fills Start Date"** (#27): `CalendarFragment.kt:323-336`. Expected: tvDate = tapped day.
- **"View mode survives a Home unit round trip"** (#12): Home reuses the fragment by tag (`bottomNav/home/Home.kt:349-353`). Switch to Week → another unit → Calendar. Expected: tvViewMode "Week".

### M20. Events badge counts (web #137) — P3, needs a second account
- **"Events badge goes up with new invites and down after RSVP"**: tvEventsBadge (`CalendarFragment.kt:486-490`) and tvReceivedBadge in the popup. Accepting in the detail calls `CalendarBadgeBookkeeper.onInvitationActioned` (`EventDetailBottomSheet.kt:511-516`). This covers sheet bug CAL-21.

### M21. Load and save error states (web #55, #56, #57, #60, #118, #119, #120, #121, #122, #123, #124, #125, #126) — P3
Appium cannot intercept HTTP. Use airplane mode or `driver.setConnection` with no data to cause network failures (HTTP 500 variants cannot be reproduced).
- **"Create fails offline: toast, form stays open with the title"** (#55, #118; edit variant #60): `EventFormFragment.kt:580-587`. Expected: etTitle still filled.
- **"Grid load fails offline: shows an error, not an empty month"** (#56, #119): `CalendarFragment.kt:406-410` only toggles the spinner, so Android shows an empty month. Likely fails, like web.
- **"List screens fail offline: show an error, not an empty list"** (#57, #120-124): `ReceivedEventsViewModel.kt:100`, `CreatedEventsViewModel.kt:142`, `DeclinedEventsViewModel.kt:179,191` all ignore `Resource.Error`. Likely fails; Join Call does handle it (`JoinCallEventsViewModel.kt:69`).
- **"Detail / invitee list fail offline"** (#125, #126): detail loadingContainer; invitee tvEmpty "No invitees".

### M22. Text edge cases (web #98, #100, #101) — P3
- **"Unicode, emoji, RTL and HTML title/description round-trip as plain text"**: compare the detail tvTitle/tvDescription text exactly. Scripts cannot run in TextViews, so only check text equality.
- **"Long titles: 150 chars round-trip; no client length limit"**: etTitle has no maxLength (`LAY/new_calendar_fragment_event_form.xml:~118-125`). Type 1000 chars, read the length back, and compare the 150-char title with the detail tvTitle.

### M23. Lower-value or deferred (web #40, #84, #95, #113, #116, #127, #128, #132, #135, #136) — P4
- #40 location picker → maps link: feature exists (MapPickerActivity, detail tvLocation opens Google Maps `EventDetailBottomSheet.kt:328-346`). Deferred by design: the location tool is not enabled in this suite.
- #84 Join on a Line 1 event opens the meet lobby: `JoinCallHelper.join` (`EventDetailBottomSheet.kt:396-404`). Risky; only assert that the lobby screen opens, then back out.
- #95, #135, #136 live updates when the organizer edits, cancels or un-invites (two accounts; RefreshBus/socket). An already-open detail sheet is not refreshed, so #135 likely fails as on web.
- #113 timezone search with no match: tvEmpty "No timezones match your search" (`LAY/new_calendar_fragment_timezone_picker.xml`).
- #116 double-tap Create makes one event: btnSave is disabled while submitting plus LoadingDialog (`EventFormFragment.kt:408-415`). Covers CAL-32.
- #127, #128 dark/light theming of the calendar screens: Android uses values-night colours (`NC/ui/theme/AppColors.kt:9-18`, ZL-21467). Switch with `adb shell cmd uimode night yes|no` and screenshot.
- #132 event created in Email → Calendar appears on Home → Calendar: same fragment (`bottomNav/new_email/ui/calendar/EmailCalendarActivity.kt:35`).

---

## CONFLICT

### X1. `deleteRecurringAll` asserts the series disappears (web #8)
- Android test: `CalendarTest.java:548-562` asserts `!agendaHasEvent(recurringTitle)`.
- Source: cancelled rows stay on the grid as "Cancelled: <title>" with strike-through (`NC/ui/calendar/DayAgendaAdapter.kt:38-41`, `HourlyTimelineAdapter.kt:89-92`). `agendaHasEvent` is a `textContains` match (`pages/common/BasePage.java:528-533`), so "Cancelled: QA Recur ... EDIT" still matches the title. The suite's own `deleteEvent` (`CalendarTest.java:476-496`) asserts the opposite for a one-off. Web #8/#62 and sheet bug CAL-02 also show cancelled rows remaining.
- Right per source: after "All events" delete, the occurrences show "Cancelled: <title>". Change the assertion to look for the "Cancelled: " prefix and check that no live, unprefixed row remains. Keep it separate from the product question in CAL-01/02 (whether cancelled rows should stay at all).

### X2. `recurringWithoutEndRejected` tests a state the UI cannot reach (web #52)
- Android test: `CalendarTest.java:262-274` picks "Daily" and expects the save to be blocked by a missing repeat-end date.
- Source: picking a frequency auto-fills Repeat Until (`EventFormViewModel.kt:318-321` → `EventFormLogic.kt:272-281`), and tvRecurrenceEnd has no clear control. The `"Repeat end date is required"` branch (`EventFormLogic.kt:98`) is unreachable from the UI. The test passes only because the ladder form is still a Members event with no invitees (`EventFormLogic.kt:104`, set by `membersNoInviteesRejected`). Web #64 agrees that the end is pre-filled; web #52 needs a clear (×) button Android lacks.
- Right per source: after "Daily", tvRecurrenceEnd is pre-filled with Start + 1 day. Replace with "Repeat Until before Start is refused" (M7, `EventFormFragment.kt:179-191`) on a Personal form, so no other rule can block the save.

### X3. `EmailCalendarTest` targets the legacy calendar (web #88)
- Android test: `tests/email/EmailCalendarTest.java:63-110` ("Email (legacy) calendar loads ...") and `EmailCalendarPage` use legacy ids `calendarView`, `tvMonthYear`, `ivNextMonth`, `btnWeek`/`btnMonth`, `fabMenuHolder`/`createEventHolder`.
- Source: the Email drawer opens `EmailCalendarActivity` (`bottomNav/new_email/ui/folders/FolderDrawerFragment.kt:87-89`), which hosts the new `CalendarFragment` (`EmailCalendarActivity.kt:15-38`), with ids `fab`, `tvMonthTitle`, `btnNextMonth`/`btnPrevMonth`, `btnViewMode`. Web #88 agrees that Email shows the new calendar.
- Right per source: the new calendar. Repoint `EmailCalendarPage` to the `CalendarPage` ids (or reuse `CalendarPage`). As written the cases will skip or fail.

Minor (not counted as conflicts):
- `repeatDialogOptions` (`CalendarTest.java:276-283`) claims to check Daily/Weekly/Monthly/Yearly/Custom but only checks Weekly.
- `personalTypeNoInvitees` (`:300-312`) uses `membersHasInvitees || !personalHasInvitees`, which passes whenever Members shows the control.
- `quickActionsPresent` uses OR.
- `emptyTitleRejected`'s toast text matches Android (`EventFormLogic.kt:75`); web shows "Title is required" (`STR:5564` exists but is unused by the new form). This is a platform wording difference to raise with devs, not a test bug.

---

## COVERED (web → Android)

Most are partial; the gap is noted in brackets.
- #1 Calendar tab renders → `calendarLoads` [no view-selector or title assert]
- #2 view switch Month/Week/Day → `weekViewToggle` [Week only, no Day; only asserts the calendar is still loaded]
- #3 prev/next/Today → `monthNavigation` [does not assert Today restores the title]
- #6 title required / ≥ 3 chars → `emptyTitleRejected`, `shortTitleRejected`
- #7 create Personal, then cancel (soft-delete) → `createPersonalEvent` + `deleteEvent` [timed, not all-day]
- #9 edit title/description/colour → `editEvent` [title only]
- #33 colour menu presets + custom → `colorPicker` [preset swatch only; "Select custom color" not opened]
- #41 Cancel discards typed data, and #117 Escape closes form (Android back) → `backReturnsToCalendar` [does not reopen and assert etTitle empty]
- #43 detail closes with × → `viewEventDetail` (`detailClose` uses btnClose) [closed state not asserted]
- #47 Members needs invitee → `membersNoInviteesRejected` [the call-type-required half is unreachable on Android: defaults to Audio Call]
- #62 cancelled event stays as "Cancelled: <title>" → `deleteEvent` [detail strike-through, "This event has been cancelled" and hidden Edit/Delete not asserted]
- #69 edit "All events" → `updateRecurringAll` [title only; frequency change not tested]
- #110 All Day hides times / Does not repeat hides repeat fields → `allDayHidesTimePickers` [Repeat part not tested]
- #112 Repeat and Reminder option lists → `repeatDialogOptions` (Repeat) + `reminderPicker` [no exact-list assert; lists are at `EventFormFragment.kt:147` and `NC/util/Constants.kt:25-32`]

## WEB-ONLY (absent on Android)

- #13 exactly one calendar mounted with a data-theme attribute: DOM-level; no Android equivalent.
- #14 Home calendar has no own theme toggle (rail owns it): there is no theme control in the Android header (`CalendarFragment.kt:295-298` hides user switcher and settings; `LAY/new_calendar_layout_calendar_header.xml`).
- #18 compact header at 1000 px keeps accessible names: browser width breakpoint.
- #19 "+N more" day popover: month cells only show a count (`LAY/new_calendar_item_month_day_cell.xml`: bgCircle, tvDayNumber, tvEventCount); the agenda list shows the day's events.
- #22 multi-day drag-select: no drag-select; a cell is click-only (`MonthGridAdapter.kt:68`).
- #23 Week all-day row: Android Week is a one-row day strip, no all-day row or time grid (`CalendarViewModel.kt:66-84`).
- #24 drag an event chip to another day: no drag handling; chip click only (`HourlyTimelineAdapter.kt:108`).
- #28 Day-view slot click pre-fills 30 minutes: Day hourly rows have no slot-click handler (`HourlyTimelineAdapter.kt:108`); the form opens only from the FAB with start = now rounded and end = start + 1 h (`EventFormViewModel.kt:29-30`).
- #76 filter (funnel) button in Received: no such control (`LAY/new_calendar_fragment_received_events.xml` has tabs, cbToday, btnDatePicker, etSearch only).
- #85 rail sun/moon theme toggle: web Home rail control. Android follows app night mode (see M23 for a dark-mode case).
- #86 past-date warning dialog in dark theme: Android has no "You cannot add events on previous date." dialog (grep finds no such string in `NC/` or `STR`).
- #87 stand-alone /calendar route: no URL routes.
- #89 production /calendar is the legacy newCalendar: web route/environment check.
- #108 repeat count field ("Number of Days/Weeks/...", min/max): no count input in `LAY/new_calendar_fragment_event_form.xml` (Repeat has tvRecurrence, chipGroupDays and tvRecurrenceEnd only).
- #129 Classic vs Windowed view: web window-manager mode.
- #130 768 px no sideways scroll: browser viewport.
- #131 event made in the /calendar window shows on Home: no stand-alone calendar window (the Email-calendar equivalent #132 is in M23).

## Sheet bugs relevant to Android

Would an Android case catch it?
- **Yes, likely reproduces on Android:**
  - CAL-01, CAL-02, CAL-04, CAL-08: "Cancelled:" rows stay and count. Android draws them by design (`DayAgendaAdapter.kt:38-41`; tvEventCount). Whether that is a bug is a product call; it is also tied to conflict X1.
  - CAL-06: decliner row without a name (M5 Request Reason case).
  - CAL-07: decline label waits for a refetch (M5 decline case).
  - CAL-03: duplicate device-calendar rows. Android mirrors to the device calendar (`NC/sync/`). Manual check only.
- **Android case would confirm it is not affected:**
  - CAL-09: reminder hidden when None (M10).
  - CAL-17: expiry computed per open (M9).
  - CAL-21: badge cleared on accept from the detail (M20).
  - CAL-25: admin override exists (M14).
  - CAL-29: Declined rows prefixed and struck through; fetch uses hide_rejected=true (`NC/data/repository/EventRepository.kt:31`).
  - CAL-31: 12-hour clock (`NC/domain/util/DateUtils.kt:13`).
  - CAL-32: Create is disabled while submitting (M23 #116).
  - CAL-33: cancelled events are kept, not removed.
  - CAL-34: Join is hidden after decline (`EventDetailBottomSheet.kt:388-392`).
- **CAL-10** ("beed" typo): Android shows the server label `calendar_event_edited` via `getDataFromLabelKey` (`EventFormFragment.kt:571-577`), so the typo may surface, but the toast is not readable on Android 12+.
- **CAL-12, CAL-23**: Android has the scope dialog and all lists (M2, M6).
- **CAL-14**: does not apply in that form, because Android groups by start date. It has the opposite gap: overnight events are missing on day 2 (M12).
- **Not applicable (desktop drag, keyboard, panels, paging internals):** CAL-11, 13, 15, 16, 18, 19, 20, 22, 24, 26, 27, 28, 30, 35, 36. CAL-20 (stale rows on tab switch) could be probed offline with M21.
