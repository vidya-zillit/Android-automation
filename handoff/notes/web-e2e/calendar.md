# Web E2E — Calendar

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: deep — Views, create/edit/cancel, recurrence, drawers, RSVP, rights, two users

## Web test cases (140)

### Calendar

- **Calendar tab renders the CalendarV3 shell @smoke** — passed (calendar.spec.js:180)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the sandbox project and go to Home → Click the Calendar tab in the unit rail → Wait for the month grid to finish loading → Check that the month grid is shown → Check that the header shows the period title (month and year) → Check that the view selector (Month / Week / Day) is shown
  - Expected: The Calendar tab opens on the month grid with its header title and the view selector.

- **view switch Month → Week → Day @smoke** — passed (calendar.spec.js:188)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Check that the month grid is shown → Open the view selector and choose Week → Check that the hourly time grid is shown → Open the view selector and choose Day → Check that the hourly time grid is shown → Open the view selector and choose Month → Check that the month grid is shown again
  - Expected: Week and Day each show the hourly time grid, and Month brings the month grid back.

- **prev / next / Today navigation changes the header title @smoke** — passed (calendar.spec.js:223)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Note the period title in the calendar header → Click the Next period arrow → Check that the header title changes → Click Today → Check that the header title is back to the one you noted
  - Expected: Next moves the calendar to another period (the title changes) and Today brings back the current period's title.

- **the Events menu opens with Received/Created/Declined @smoke** — passed (calendar.spec.js:237)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the Events menu button in the calendar header → Check that the menu lists Received Events → Check that the menu lists Created Events → Check that the menu lists Declined Events → Press Esc to close the menu
  - Expected: The Events menu offers Received Events, Created Events and Declined Events.

- **the Received Events drawer opens and closes @smoke** — passed (calendar.spec.js:251)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the Events menu button → Choose Received Events → Check that the Received Events drawer opens → Check that the drawer shows the Pending tab → Press Esc → Check that the drawer closes
  - Expected: The Received Events drawer opens with its Pending tab and closes with Esc.

- **validation: title is required and must be >= 3 chars @smoke** — passed (calendar.spec.js:331)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button to open the New Event form → Leave the title empty and click Create Event → Check that “Title is required” is shown → Type “ab” in Add title and click Create Event → Check that an error saying the title needs at least 3 characters is shown → Click Cancel and check that the form closes
  - Expected: An empty title shows “Title is required” and a two-letter title shows the at-least-3-characters error; nothing is created.

- **create a Personal all-day event, then cancel it (soft-delete)** — passed (calendar.spec.js:306)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button to open the New Event form → Type a unique title such as “[e2e] event …” in Add title → Choose Personal and turn All Day on (date stays today) → Click Create Event and check that a success message appears → Click the new event on the grid (open “+N more” on today if it is collapsed) → In Event Details, click Delete → In the “Delete event” confirm, click Delete → Check that a success message (event cancelled) appears
  - Expected: The all-day event is created for today and then cancelled from its detail, each step confirmed by a success message.

- **create a Weekly recurring event, then cancel the whole series** — passed (calendar.spec.js:357)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] recurring …”, choose Personal and keep All Day on for today → Set Repeat to Weekly and press Ok on the recurrence notice if it appears → Click Create Event and check that a success message appears → Click the new event on the grid → Check that Event Details says the event is Recurring → Click Delete → In “Delete recurring event”, choose “All events in this series” and click Delete → Check that a success message appears
  - Expected: A weekly all-day series is created, its detail is marked Recurring, and the whole series is cancelled with a success message.

- **edit an event: change title, description and color** — passed (calendar.spec.js:385)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: Creates a Personal all-day event for today, “[e2e] edit …”, through the New Event form.
  - Steps: Click the event on the grid and, in Event Details, click Edit Event → Change the title to the same title followed by “ EDITED” → Type “e2e edited description” in the description box → Click the Color button, choose “Select from preset colors” and pick the second colour in “Choose a Color” → Click Update Event and check that a success message appears → Check that the renamed event is on the grid → Open the renamed event, click Delete and confirm Delete → Check that a success message appears
  - Expected: The edit saves (success message) and the event shows on the grid under its new title. The reminder is left alone on purpose.

- **create a Members event with the organizer excluded** — passed (calendar.spec.js:438)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button and type a unique title such as “[e2e] members …” → Choose Members and turn All Day on (date stays today) → Set Call Type to Audio Call → Click Add Invitees, search “Lava Device”, pick that row and click Done → Tick “The organizer will not be a part of this event” → Click Create Event and check that a success message appears → Open Events menu → Created Events, search the title and click “View full event details” → In Event Details, click Delete, confirm Delete and check that a success message appears
  - Expected: The Members event is created without the organizer; it is found through Created Events (it is not on the creator's own grid) and can be cancelled from there.

- **a pending received invite is actionable (RSVP surface)** — passed (calendar.spec.js:491)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the Events menu button and choose Received Events → Click the Pending tab → Wait until the list shows either “No pending events” or invite cards → Check that an invite card shows its “View full event details” link
  - Expected: A pending invite card is listed with its “View full event details” link (where Accept and Decline live). Nothing is accepted or declined.

### Calendar — shell & navigation

- **the view survives a unit-tab round trip @smoke** — passed (calendar.spec.js:534)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Switch the view selector to Week → Click the Bulletin tab in the unit rail → Check that the calendar is no longer shown → Click the Calendar tab again → Check that the Week grid is shown → Check that the view selector reads Week
  - Expected: After leaving for Bulletin and coming back, the calendar is still in Week view and the selector says Week.

- **exactly one calendar is mounted, with a theme attribute @smoke** — passed (calendar.spec.js:548)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Close any open Email or Calendar windows, then open the Calendar tab → Check that exactly one calendar is on the page → Check that the calendar is set to either the light or the dark theme
  - Expected: Only one calendar is present and it carries a light or dark theme setting.

- **the Home calendar has no own theme toggle (the rail owns it) @smoke** — passed (calendar.spec.js:556)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Look at the calendar header → Check that it has no “Toggle theme” button → Check that the theme toggle on Home's unit rail is shown
  - Expected: The calendar inside Home has no theme button of its own; the unit rail's theme toggle is the one shown.

- **each navigation refetches the grid with the new range @smoke** — passed (calendar.spec.js:566)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: In Month view, click the Next period arrow and check that events are reloaded for a new date range that hides declined events → Click Today and check that events are reloaded for an earlier range → Switch to Week and wait for the events to reload → Click Next period and check that the reloaded range spans exactly 7 days → Click Today and check that the range moves earlier → Switch to Day, click Next period and check that the range spans exactly 1 day → Click Today and check that the range moves earlier
  - Expected: Every Next and Today reloads the events for the period on screen (Week = 7 days, Day = 1 day), always with declined events hidden.

- **the view options are exactly Month / Week / Day @smoke** — passed (calendar.spec.js:597)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the view selector → Check that the options are exactly Month, Week and Day, in that order → Press Esc to close the list
  - Expected: The view selector offers only Month, Week and Day (no Agenda or List view).

- **today is marked in Month, Week and Day @smoke** — flaky (calendar.spec.js:608)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: In Month view, check that today's cell is the one highlighted as today → Switch to Week → Check that the current-time line is drawn → Switch to Day → Check that the day shown is today
  - Expected: Today is highlighted in Month, Week shows the current-time line, and Day shows today's column.
  - Web error: Error: expect(locator).toBeVisible() failed

- **the compact header keeps its accessible names @smoke** — passed (calendar.spec.js:626)
  - Where: Home → Calendar tab, with the browser window 1000 px wide · account: iPhone XRR
  - Steps: Make the browser window 1000 × 760 so the calendar header turns compact → Open the Calendar tab → Check that the Events menu button is shown as an icon without the word “Events” → Check that the Join Events Call button is shown as an icon without its text → Check that both buttons still carry their names “Events menu” and “Join Events Call” for screen readers
  - Expected: In the compact header both buttons are icon-only but keep their accessible names.

### Calendar — grid

- **"+N more" opens the day popover and its events @smoke** — passed (calendar.spec.js:649)
  - Where: Home → Calendar tab, Month view of the current month · account: iPhone XRR
  - Steps: Find a day with a “+N more” link → Click the first “+N more” link → Check that the day popover opens with its events → Click the first event in the popover → Check that Event Details opens with the event title → Press Esc and check that the detail closes
  - Expected: “+N more” opens a popover listing that day's events, and an event in it opens its Event Details.

- **selecting a past day warns instead of opening the form @smoke** — passed (calendar.spec.js:668)
  - Where: Home → Calendar tab, Month view · account: iPhone XRR
  - Steps: Find a past day in the month (go to the previous month if the current one has none) → Click the most recent past day → Check that the warning “You cannot add events on previous date.” appears → Check that no New Event form opens → Click Ok and check that the warning closes
  - Expected: Clicking a past day shows “You cannot add events on previous date.” and does not open the New Event form.

- **a past slot earlier today must not be saved (intercepted)** — suspected · known bug Week/Day slot select only refuses days before today; a past slot today opens the form and the POST is sent (no past-start check),Week/Day slot select only refuses days before today; a past slot today opens the form and the POST is sent (no past-start check),Week/Day slot select only refuses days before today; a past slot today opens the form and the POST is sent (no past-start check) (calendar.spec.js:689)
  - Where: Home → Calendar tab → Day view on today · account: iPhone XRR
  - Setup: The test catches every calendar save and answers it with a failure itself, so nothing reaches the server.
  - Steps: Switch to Day view on today → Click the 12:00 AM slot (already past) → Check that the New Event form opens with Start Time 12:00 AM → Type a unique title such as “[e2e] past slot …” and choose Personal → Click Create Event and wait 3 seconds → Check that no save was attempted
  - Expected: A start time that has already passed today is refused in the form, before any save is sent. Known bug: the past slot opens the form and the save is sent anyway (there is no past-start check).
  - Web error: Test timeout of 90000ms exceeded.

- **a multi-day drag-select opens the form on the first day @smoke** — failed (calendar.spec.js:727)
  - Where: Home → Calendar tab → Month view of next month · account: iPhone XRR
  - Steps: Switch to Month view and click Next period to show next month → Press on the 10th of next month and drag to the 12th, then release → Check that the New Event form opens → Check that Start Date is the 10th → Check that End Date is also the 10th → Click Cancel and check that the form closes
  - Expected: Dragging across several days opens the form on the first day only (Start and End Date both the 10th); there is no multi-day range.
  - Web error: Error: expect(locator).toBeVisible() failed

- **selecting in the Week all-day row opens an all-day form @smoke** — suspected · known bug handleSelect always pre-fills full_day:false — the all-day row opens a timed form,handleSelect always pre-fills full_day:false — the all-day row opens a timed form,handleSelect always pre-fills full_day:false — the all-day row opens a timed form (calendar.spec.js:752)
  - Where: Home → Calendar tab → Week view, two weeks ahead · account: iPhone XRR
  - Steps: Switch to Week view → Click Next period twice → Click the first day's cell in the all-day row at the top of the week → Check that the New Event form opens → Check that the All Day switch is on → Click Cancel
  - Expected: Clicking the all-day row opens the form with All Day already on. Known bug: the form always opens as a timed event (All Day off).
  - Web error: Error: expect(locator).toBeVisible() failed

- **dragging a chip to another day is saved** — suspected · known bug FullCalendar editable=true but no eventDrop/eventResize handler: a drag moves the chip and snaps back on refetch,FullCalendar editable=true but no eventDrop/eventResize handler: a drag moves the chip and snaps back on refetch,FullCalendar editable=true but no eventDrop/eventResize handler: a drag moves the chip and snaps back on refetch (calendar.spec.js:778)
  - Where: Home → Calendar tab → Week view of the week three days ahead · account: iPhone XRR
  - Setup: Creates a Personal timed event “[e2e] drag …” three days from today at 7:00 AM (one hour) through the New Event form.
  - Steps: Switch to Week view on the week holding the event → Drag the event's chip sideways to the next day (or the previous day when the event is on a Saturday, to stay in the same week) → Release and wait a few seconds → Click Next period, then Previous period, to reload the week → Check that the move was saved → Check that the chip now sits on the day it was dropped on
  - Expected: The drop is saved and the event stays on its new day after the week reloads. Known bug: nothing is saved, so the chip snaps back to its old day on reload.
  - Web error: Error: a drop must send a save request

- **an expired event opens read-only @smoke** — passed (calendar.spec.js:835)
  - Where: Home → Calendar tab, Month view · account: iPhone XRR
  - Steps: Find an event on a past day in this month (go to the previous month if there is none) → Click that event → Check that Event Details says “This event has expired” → Check that there is no Edit Event, Delete or Join button → Press Esc to close the detail
  - Expected: A past event opens read-only: it says “This event has expired” and offers no Edit Event, Delete or Join.

### Calendar — create

- **Personal timed event via the FAB — payload, chip, detail (C1, D18, F1)** — failed (calendar.spec.js:875)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button and type a unique title such as “[e2e] timed …” with two spaces before and after it → Choose Personal, set Start Date to two days from today and turn All Day off → Pick Start Time 10:00 AM and check that End Time changes to 11:00 AM by itself → Check that Call Type, Invitees and External Guests are not shown → Click Create Event and check that the event is saved with the title trimmed, as Personal, no call, 10:00–11:00 AM India time, the India timezone, no invitees, no reminder and no call line → Switch to Day view two days ahead and check that the chip's tooltip reads “<time> — <title>” → Open the event and check Event Details: the title, “Personal”, “Duration: 1 hours” and “Week N of the year” → Check that the detail has no See invitees, Join or Accept button and no DESCRIPTION block → Click Delete, confirm Delete and check that a success message appears
  - Expected: The Personal event saves with the trimmed title at 10:00–11:00 AM, no call and no invitees; its chip and detail show it with none of the Members-only parts.
  - Web error: Test timeout of 90000ms exceeded.

- **selecting a future month cell pre-fills the Start Date @smoke** — passed (calendar.spec.js:935)
  - Where: Home → Calendar tab → Month view of next month · account: iPhone XRR
  - Steps: Switch to Month view and click Next period until the 15th of next month is shown → Click that day (the 15th of next month) → Check that the New Event form opens with Start Date set to that day → Check that the All Day switch is off → Click Cancel and check that the form closes
  - Expected: Clicking a future day opens the New Event form with that day as Start Date and All Day off.

- **selecting a Day-view slot pre-fills a 30-minute time @smoke** — passed (calendar.spec.js:953)
  - Where: Home → Calendar tab → Day view two days from today · account: iPhone XRR
  - Steps: Switch to Day view, click Today, then click Next period twice → Scroll to the 6:00 AM slot and click it → Check that the New Event form opens with Start Date two days from today → Check that Start Time is 6:00 AM and End Time is 6:30 AM → Click Cancel and check that the form closes
  - Expected: Clicking a Day-view slot opens the form on that day with a 30-minute time, 6:00 AM to 6:30 AM.

- **an overnight event ends on the next day** — passed (calendar.spec.js:972)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] overnight …” and choose Personal → Set Start Date to four days from today and turn All Day off → Pick Start Time 11:00 PM and End Time 12:30 AM → Check that End Date changes to the next day (five days from today) → Click Create Event and check that it is saved from 11:00 PM on day four to 12:30 AM on day five, India time → Switch to Day view four days ahead and check that the event is shown → Go to the next day in Day view and check that the event is shown there too
  - Expected: An event from 11:00 PM to 12:30 AM ends on the following day and appears on both days.

- **another timezone converts the fields and keeps the instant** — failed (calendar.spec.js:994)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] timezone …” and choose Personal → Set Start Date to three days from today, turn All Day off and pick 10:00 AM to 11:00 AM → Open Timezone, type “York” and pick the first match (a New York zone) → Check that Start Time no longer reads 10:00 AM (it is converted to New York time) → Click Create Event and check that it is saved in the New York timezone but still at 10:00–11:00 AM India time → Switch to Day view three days ahead and check that the chip starts at 10:00 → Open the event and check that the detail reads “10:00 AM - 11:00 AM” → Click Delete, confirm Delete and check that a success message appears
  - Expected: Changing the timezone converts the time fields but keeps the same moment: the event is still 10:00–11:00 AM in the browser's India time on the grid and in the detail.
  - Web error: Test timeout of 90000ms exceeded.

- **a drawer card shows the same time as the detail** — suspected · known bug EventCard formats times in event.timezone (no label) while the detail modal uses the browser zone,EventCard formats times in event.timezone (no label) while the detail modal uses the browser zone,EventCard formats times in event.timezone (no label) while the detail modal uses the browser zone (calendar.spec.js:1045)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] tz card …” and choose Personal → Set Start Date to three days from today, turn All Day off and pick 2:00 PM to 3:00 PM → Open Timezone, type “York” and pick the first match (a New York zone) → Click Create Event → Open Events menu → Created Events, choose the Personal tab and search the title → Check that the event's card shows “2:00 PM - 3:00 PM”
  - Expected: The Created Events card shows the same browser-time (India) range as the detail, 2:00 PM - 3:00 PM. Known bug: the card shows the time in the event's own timezone (New York), with no label.
  - Web error: Test timeout of 90000ms exceeded.

- **reminder and a preset colour on create (C5)** — passed (calendar.spec.js:1086)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] reminder colour …” and choose Personal → Set Start Date to two days from today, turn All Day off and pick Start Time 3:00 PM → Set Reminder to “30 minutes before” → Click the Color button, choose “Select from preset colors” and pick Ocean Blue → Check that the Color field shows #5B7FA5 → Click Create Event and check that it is saved with a 30-minute reminder and colour #5B7FA5 → Switch to Day view two days ahead and check that the chip is Ocean Blue → Open the event and check that the detail shows “30 minutes before” → Click Delete, confirm Delete and check that a success message appears
  - Expected: The reminder and the Ocean Blue colour are saved; the chip is drawn in Ocean Blue and the detail shows “30 minutes before”.

- **the colour menu offers presets and a custom picker @smoke** — passed (calendar.spec.js:1121)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button to open the New Event form → Click the Color button → Check that “Select from preset colors” is offered → Click “Select custom color” → Check that the Custom Color dialog opens with a colour picker → Close the dialog with × and check that it closes → Click Cancel on the form
  - Expected: The colour menu offers preset colours and a custom colour picker dialog; nothing is saved.

- **Members audio event with the pinned invitee (F2, K1, N1, L6)** — passed (calendar.spec.js:1138)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] members audio …”, choose Members, set Start Date four days ahead, turn All Day off and pick 10:00 AM → Set Call Type to Audio Call and check that Call Line shows Line 1 → Click Add Invitees, search “Lava Device”, pick that row, click Done and check that the form says “1 person invited” → Click Create Event and check that it is saved as a Members audio call on Line 1, organizer included, with one project member invited → Switch to Day view four days ahead, open the event and check that the detail shows Members, Audio Call and the number of invitees → Check that a Join button is shown (it is not clicked) and that there is no Accept or Decline button → Click See invitees and check that the list shows Lava Device and “1 Pending” → Search the list for a name nobody has, check “No invitees match your search”, then press Esc → Click Delete, confirm Delete and check that a success message appears
  - Expected: The audio event saves on Line 1 with the organizer and Lava Device; the organizer's detail offers Join but no RSVP buttons, and the invitee list shows Lava Device as 1 Pending.

- **call types drive the Call Line and Location fields @smoke** — passed (calendar.spec.js:1198)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button and choose Members → Open Call Type and check that the options are exactly Audio Call, Video Call, Meet in Person and Meet in Person & Call → Pick Meet in Person and check that no Call Line field is shown → Pick Video Call and check that Call Line is shown → Pick Meet in Person & Call and check that Call Line is shown and Location is marked required (*) → Choose Personal and check that Call Type, Invitees, External Guests and “The organizer will not be a part of this event” are gone → Click Cancel and check that the form closes
  - Expected: Call Line appears only for call types with a call, Location becomes required for Meet in Person & Call, and Personal hides every Members-only field.

- **a Line 2 video event keeps Line 2 when reopened** — failed (calendar.spec.js:1233)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] line 2 …”, choose Members, set Start Date five days ahead, turn All Day off and pick 11:00 AM → Set Call Type to Video Call and Call Line to Line 2 → Click Add External Guests, type an address such as “e2e+…@example.com”, click Add, answer No to “Add User”, then click Done → Click Create Event and check that it is saved as a video call on Line 2 with only that external guest → Switch to Day view five days ahead, open the event and click Edit Event (never Join) → Check that Call Line reads Line 2 → Click Cancel and check that the form closes
  - Expected: A Line 2 video event is saved on Line 2 and the edit form shows Line 2 when it is reopened.
  - Web error: Error: expect(locator).toBeVisible() failed

- **an external guest only (meet in person) — J11, N1** — failed (calendar.spec.js:1261)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] guest …”, choose Members, set Start Date six days ahead, turn All Day off and pick 9:00 AM → Set Call Type to Meet in Person → Click Add External Guests, add an address such as “e2e+…@example.com”, answer No to “Add User”, click Done and check that the form says “1 external guest” → Click Create Event and check that it is saved as meet in person, with no call line and only that guest → Switch to Day view six days ahead, open the event and check that it shows Meet in Person and has no Join button, then press Esc → Open Events menu → Created Events (Invites Sent) and search the title → On the event's card, click “See invitees” → Check that the list shows an External Guests section with the guest's email, then press Esc and close the drawer
  - Expected: An event with only an external guest saves as meet in person with no call, its detail offers no Join, and the Created Events card lists the guest under External Guests.
  - Web error: Error: expect(locator).toBeVisible() failed

- **external email validation in the guest modal @smoke** — notrun (calendar.spec.js:1306)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, choose Members and click Add External Guests → Type “not-an-email” and click Add; check the error “Please enter a valid email address” → Type an address such as “e2e+…@example.com”, click Add, answer No to “Add User” and check that the address is listed → Type the same address again and click Add; check the warning “Email already selected.” → Click Remove on the address and check “No external guests added yet” → Click Cancel in the External Guests dialog, then Cancel on the form
  - Expected: An invalid address is refused, a duplicate address is warned about, and a removed guest leaves the empty message; nothing is saved.

- **the invitee picker modes, search and select-all @smoke** — notrun (calendar.spec.js:1344)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, choose Members and click Add Invitees → Check that Select Invitees offers the Departments and Select Users modes → In Select Users, search a name nobody has and check “No users found” → If All Departments is offered (admins only): choose it and check “Invite all N team members” and “Everyone in the project will be invited” → Then click Select All and check that Deselect All appears and Done shows a count above 0 → Then click Deselect All and check that the button reads “Done (0)” → Choose Departments and check that the “Search departments...” box appears → Click Cancel in the picker, then Cancel on the form
  - Expected: The invitee picker offers its modes, shows “No users found” for an unknown name, and (for admins) All Departments selects and clears everyone; nothing is saved.

- **a picked location is saved and links to Maps (F8)** — notrun (calendar.spec.js:1388)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] location …” and choose Personal → Set Start Date six days ahead, turn All Day off and pick Start Time 1:00 PM → Check that the Location button reads “Select location” and click it → In the Select Location map, wait until Select is enabled and click it (keeps the map's default spot) → Check that the Location button now shows the picked place → Click Create Event and check that the event is saved with a place description and map coordinates → Switch to Day view six days ahead and open the event → Check that the detail shows a Google Maps link that opens in a new tab → Click Delete, confirm Delete and check that a success message appears
  - Expected: The picked location is saved with the event and its detail links to Google Maps in a new tab.

- **cancelling the form discards what was typed @smoke** — notrun (calendar.spec.js:1432)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button and type “discard me” in Add title → Click Cancel and check that the form closes → Click the + button again → Check that the title field is empty → Click Cancel
  - Expected: Cancelling discards the typed title: the next New Event form opens empty.

### Calendar — event detail

- **a 65-minute event does not read "1.5 hours"** — notrun (calendar.spec.js:1761)
  - Where: Home → Calendar tab → Day view two days from today · account: iPhone XRR
  - Setup: Creates a Personal event “[e2e] duration …” two days from today, 12:00 PM to 1:05 PM, through the New Event form.
  - Steps: Switch to Day view two days from today → Click the event to open Event Details → Check that the detail shows a “Duration:” line → Check that the duration does not read “1.5 hours” → Click Delete, confirm Delete and check that a success message appears
  - Expected: A 65-minute event's duration is not shown as “1.5 hours”. Known bug: 1 hour 5 minutes is written as “1.5 hours”.

- **the detail closes with Esc and with × @smoke** — notrun (calendar.spec.js:1784)
  - Where: Home → Calendar tab, Month view of the current month · account: iPhone XRR
  - Steps: Click the first visible event in the month → Check that Event Details opens with the event title → Press Esc and check that the detail closes → Click the same event again and check that the detail opens → Click × in the detail and check that it closes
  - Expected: Event Details closes both with Esc and with its × button.

- **a Box Schedule event cannot be edited or deleted here @smoke** — notrun (calendar.spec.js:1802)
  - Where: Home → Calendar tab, Month view · account: iPhone XRR
  - Steps: Look in next month, then this month, for a future, not-cancelled event that comes from Box Schedule → Click that event to open Event Details → Click Edit Event → Check that a “Box Schedule Event” notice says it can only be edited or deleted from there → Click Got it and check that no edit form opens → Open the event again and click Delete → Check that the same notice appears, click Got it and check that no “Delete event” confirm opens
  - Expected: Edit and Delete on a Box Schedule event only show a notice pointing to Box Schedule; no edit form or delete confirm opens.

- **an event that ended while the calendar was open reads as expired** — notrun (calendar.spec.js:1843)
  - Where: Home → Calendar tab → Day view on today · account: iPhone XRR
  - Setup: Creates a 15-minute Personal event “[e2e] frozen now …” later today, starting at the first quarter hour at least 20 minutes from now. The test controls the page's clock.
  - Steps: Switch to Day view on today and check that the event is shown → Without reloading, move the page's clock to 10 minutes after the event ends → Click the event to open Event Details → Check that the detail says “This event has expired” → Check that there is no Edit Event button → Press Esc, then set the page's clock back to the real time
  - Expected: An event that ended while the calendar stayed open reads as expired and can no longer be edited. Known bug: the calendar keeps the time from when it was opened, so it still offers Edit, Join and Accept on ended events.

### Calendar — validation

- **a whitespace-only title is refused client-side (intercepted)** — notrun (calendar.spec.js:1452)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test catches every calendar save and answers it with a failure itself, so nothing reaches the server.
  - Steps: Click the + button and type three spaces as the title → Choose Personal, set Start Date two days ahead and turn All Day on → Click Create Event and wait a few seconds → Check that no save was attempted → Check that the form shows “Title is required” or the at-least-3-characters error
  - Expected: A title made only of spaces is refused in the form with a title error, and nothing is sent. Known bug: the form accepts it, the spaces are trimmed to an empty title and only the server refuses it.

- **a Members event needs a call type and an invitee @smoke** — notrun (calendar.spec.js:1482)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: As a safety net the test blocks every calendar save, so nothing can reach the server.
  - Steps: Click the + button and type “abc e2e” as the title → Choose Members → Click Create Event without a call type or invitee → Check that “Call type is required” is shown → Check that “At least one invitee or external guest is required” is shown → Check that no save was attempted, then click Cancel
  - Expected: A Members event without a call type and without any invitee or guest is refused with both messages, and nothing is sent.

- **Meet in Person & Call requires a location @smoke** — notrun (calendar.spec.js:1503)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: As a safety net the test blocks every calendar save, so nothing can reach the server.
  - Steps: Click the + button and type “abc e2e location” as the title → Choose Members and set Start Date two days ahead → Set Call Type to Meet in Person & Call → Click Add External Guests, add an address such as “e2e+…@example.com”, answer No to “Add User” and click Done → Click Create Event without picking a location → Check that the Location field says “Location not selected” → Check that no save was attempted, then click Cancel
  - Expected: Meet in Person & Call without a location is refused with “Location not selected”, and nothing is sent.

- **an event shorter than 15 minutes is refused @smoke** — notrun (calendar.spec.js:1526)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: As a safety net the test blocks every calendar save, so nothing can reach the server.
  - Steps: Click the + button, type “abc e2e short” as the title and choose Personal → Set Start Date two days ahead and turn All Day off → Pick Start Time 10:00 AM and End Time 10:10 AM → Click Create Event → Check that the error “Event must be at least 15 minutes long” appears → Check that no save was attempted, then click Cancel
  - Expected: A 10-minute event is refused with “Event must be at least 15 minutes long”, and nothing is sent.

- **a reminder that is already past is refused @smoke** — notrun (calendar.spec.js:1549)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: As a safety net the test blocks every calendar save, so nothing can reach the server.
  - Steps: Click the + button, type “abc e2e reminder” as the title and choose Personal → Turn All Day on (the date stays today, so the event starts at midnight) → Set Reminder to “5 minutes before” → Click Create Event → Check that the error “Selected reminder time has already passed” appears → Check that no save was attempted, then click Cancel
  - Expected: A reminder that falls before now (5 minutes before today's midnight start) is refused, and nothing is sent.

- **a Custom repeat needs a weekday @smoke** — notrun (calendar.spec.js:1573)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: As a safety net the test blocks every calendar save, so nothing can reach the server.
  - Steps: Click the + button, type “abc e2e custom” as the title and choose Personal → Set Start Date two days ahead and turn All Day on → Set Repeat to Custom → Check that no recurrence notice pops up and that “Select Days” is shown → Click Create Event without ticking any weekday → Check that the error “Please select at least one weekday for a custom repeat” appears → Check that no save was attempted, then click Cancel
  - Expected: Custom repeat shows the weekday picker without a notice, and saving with no weekday ticked is refused; nothing is sent.

- **a cleared Repeat Until is refused @smoke** — notrun (calendar.spec.js:1598)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: As a safety net the test blocks every calendar save, so nothing can reach the server.
  - Steps: Click the + button, type “abc e2e until” as the title and choose Personal → Set Start Date two days ahead and turn All Day on → Set Repeat to Weekly and press Ok on the recurrence notice → Check that Repeat Until shows the date nine days from today (one week after the start) → Hover Repeat Until and click its clear (×) icon → Check that the field is empty and “Repeat until date and weeks are required” is shown → Click Create Event and check the error “Repeat end date is required for recurring events” → Check that no save was attempted, then click Cancel
  - Expected: Clearing Repeat Until on a weekly event shows the required message and saving is refused; nothing is sent.

- **the date pickers disable past / too-early dates @smoke** — notrun (calendar.spec.js:1630)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button and choose Personal → Open the Start Date calendar → Check that yesterday is greyed out and tomorrow can be picked (whichever of the two shows on the open month) → Set Start Date two days ahead, turn All Day on, set Repeat to Weekly and press Ok on the notice → Open the Repeat Until calendar → Check that the start date itself is greyed out and the day after it can be picked → Click Cancel and check that the form closes
  - Expected: Start Date cannot be set in the past, and Repeat Until cannot be on or before the start date.

- **start times already past today are disabled @smoke** — notrun (calendar.spec.js:1666)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button and choose Personal (the date stays today) → Turn All Day off → Open Start Time and type “12:00 AM” → Check that the 12:00 AM option is greyed out → Click Cancel and check that the form closes
  - Expected: For today, start times that have already passed (such as 12:00 AM) cannot be picked.

- **a server failure keeps the form open with its values (intercepted)** — notrun (calendar.spec.js:1688)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test makes every calendar save fail with the server message “e2e forced failure”, so nothing reaches the server.
  - Steps: Click the + button and type a unique title such as “[e2e] forced failure …” → Choose Personal, set Start Date two days ahead and turn All Day on → Click Create Event → Check that the error message “e2e forced failure” appears → Check that exactly one save was attempted → Check that the form is still open and the title is still filled in → Click Cancel
  - Expected: When the server refuses the save, its message is shown and the form stays open with what was typed.

- **a failed /events fetch shows an error, not an empty month @smoke** — notrun (calendar.spec.js:1714)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: After the month has loaded, the test makes any further loading of events fail with a network error.
  - Steps: Open the Calendar tab and wait for the month to load → Click the Next period arrow (loading the next month now fails) → Check that the calendar shows an error message (failed, error, couldn't load or try again)
  - Expected: When the events cannot be loaded, the calendar says so instead of showing an empty month. Known bug: the failure is ignored and the month is shown empty.

- **a failed drawer fetch shows an error, not an empty list @smoke** — notrun (calendar.spec.js:1732)
  - Where: Home → Calendar tab → Events menu → Received Events · account: iPhone XRR
  - Setup: The test makes the Received Events list fail to load with a network error.
  - Steps: Click the Events menu button and choose Received Events (loading the list now fails) → Check that the drawer shows an error message (failed, error, couldn't load or try again) → Press Esc to close the drawer
  - Expected: When the received invites cannot be loaded, the drawer shows an error instead of an empty list. Known bug: the failure is shown as an empty list (“No pending events”).

### Calendar — edit & delete

- **edit moves the date and time and sends the full payload** — notrun (calendar.spec.js:1891)
  - Where: Home → Calendar tab → Day view two days from today · account: iPhone XRR
  - Setup: Creates a Personal event “[e2e] move …” two days from today, 4:00 PM to 5:00 PM, through the New Event form.
  - Steps: Switch to Day view two days ahead, open the event and click Edit Event → Check that the dialog is titled Edit Event and offers no Personal / Members choice → Set Start Date to three days ahead and Start Time to 1:00 PM → Click Update Event and check that the whole event is saved with the same title, as Personal, from 1:00 PM to 2:00 PM on the new day → Switch to Day view three days ahead and check that the event is shown → Go back to Day view two days ahead and check that the event is no longer there
  - Expected: Editing moves the event to the new day and time (keeping its one-hour length); the event type cannot be changed on edit.

- **add a project invitee on edit; the picker hides the creator** — notrun (calendar.spec.js:1926)
  - Where: Home → Calendar tab → Day view five days from today · account: iPhone XRR
  - Setup: Creates a Members event “[e2e] edit invitee …” five days from today at 2:00 PM (Meet in Person) with one external guest “e2e+…@example.com”, through the New Event form.
  - Steps: Switch to Day view five days ahead, open the event and click Edit Event → Click Add Invitees and search your own name (iPhone XRR) → Check that “No users found” is shown, then click Cancel in the picker → Click Add Invitees again, search “Lava Device”, pick that row and click Done → Click Update Event → Check that the saved invitees include both a project member and the external guest
  - Expected: The creator cannot pick themselves in the invitee picker, and a project member added on edit is saved alongside the existing external guest.

- **a failed edit rolls the optimistic rename back** — notrun (calendar.spec.js:1962)
  - Where: Home → Calendar tab → Day view three days from today · account: iPhone XRR
  - Setup: Creates a Personal event “[e2e] rollback …” three days from today at 5:00 PM through the New Event form. For the first edit, the test makes the save fail with a server error message.
  - Steps: Switch to Day view three days ahead, open the event and click Edit Event → Change the title by adding “ RB” and click Update Event (this save fails) → Check that an error message appears and the form stays open → Click Cancel → Check that no event named “… RB” is on the grid and the original title is still shown → With saves working again, open the event, click Edit Event, change the title by adding “ OK” and click Update Event → Check that the event now shows on the grid under the “… OK” title
  - Expected: A failed edit leaves the event under its original title (the quick on-screen rename is undone); a real edit renames it.

- **edit and delete from the Created Events card** — notrun (calendar.spec.js:2004)
  - Where: Home → Calendar tab → Events menu → Created Events → Personal · account: iPhone XRR
  - Setup: Creates a Personal event “[e2e] card …” three days from today at 6:00 PM through the New Event form.
  - Steps: Open Events menu → Created Events, choose Personal and search the title → Click the edit (pencil) button on the event's card → Check that the edit form opens with the event's title → Type “e2e drawer edit” in the description box and click Update Event → Check that the new description is saved → Click the delete button on the card and, in “Delete event”, click Delete → Check that the whole event is cancelled → Check that the card no longer offers Edit / Delete (or leaves the list), then close the drawer
  - Expected: The Created Events card can edit the event (the description is saved) and cancel it; afterwards the card no longer offers its actions.

- **a cancelled event stays on the grid, struck through and read-only (C3, F5)** — notrun (calendar.spec.js:2041)
  - Where: Home → Calendar tab → Day view four days from today · account: iPhone XRR
  - Setup: Creates a Personal event “[e2e] cancel …” four days from today at 7:00 PM through the New Event form.
  - Steps: Switch to Day view four days ahead and open the event → Click Delete, confirm Delete and check that a success message appears (the whole event is cancelled) → Check that the event stays on the grid, marked as cancelled and reading “Cancelled: <title>” → Open the cancelled event again → Check that its heading reads “Cancelled: <title>” and is struck through → Check that the detail says “This event has been cancelled” → Check that there is no Edit Event or Delete button
  - Expected: A cancelled event stays on the grid as “Cancelled: <title>”, and its detail is struck through, says it was cancelled, and offers no Edit or Delete.

- **cancelling the confirm and a failed delete leave the event live** — notrun (calendar.spec.js:2072)
  - Where: Home → Calendar tab → Day view five days from today · account: iPhone XRR
  - Setup: Creates a Personal event “[e2e] keep …” five days from today at 8:00 PM through the New Event form. For the second delete, the test makes the delete fail with a server error message.
  - Steps: Switch to Day view five days ahead, open the event and click Delete → Check that the confirm says “Are you sure you want to delete this event?” and click Cancel → Check that nothing was deleted and the event is still live → Open the event again, click Delete and confirm Delete (this delete fails) → Check that an error message appears → Check that the event goes back to live (not “Cancelled:”), then press Esc → With deletes working again, open the event, click Delete, confirm Delete and check that a success message appears
  - Expected: Cancel in the confirm sends nothing, and a failed delete undoes the on-screen “Cancelled” change so the event stays live.

### Calendar — recurrence

- **each frequency explains its default end and count unit @smoke** — notrun (calendar.spec.js:2135)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, choose Personal, set Start Date two days ahead and turn All Day on → Set Repeat to Daily; check the notice mentions “tomorrow as the end date”, press Ok, and check “Number of Days” and a Repeat Until of three days from today → Set Repeat to Weekly; check the notice mentions “one week as the end date”, press Ok, and check “Number of Weeks” and a Repeat Until of nine days from today → Set Repeat to Monthly; check the notice mentions “one month as the end date”, press Ok, and check “Number of Months” and a filled Repeat Until → Set Repeat to Yearly; check the notice mentions “one year as the end date”, press Ok, and check “Number of Years” and a filled Repeat Until → Set Repeat to Custom and check that no notice appears and “Select Days” and “Number of Weeks” are shown → Click Cancel and check that the form closes
  - Expected: Daily, Weekly, Monthly and Yearly each explain their default end in a notice and show the matching count unit and end date; Custom shows the weekday picker with no notice.

- **count ↔ Repeat Until stay in sync and follow the start date @smoke** — notrun (calendar.spec.js:2168)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, choose Personal, set Start Date two days ahead and turn All Day on → Set Repeat to Weekly, press Ok on the notice, and check that Repeat Until is nine days from today → Type 3 in Number of Weeks and check that Repeat Until changes to 23 days from today → Set Repeat Until to 16 days from today and check that Number of Weeks changes to 2 → Change Start Date to four days ahead and check that Repeat Until resets to 11 days from today → Click Cancel and check that the form closes
  - Expected: The repeat count and Repeat Until update each other, and a new start date resets Repeat Until to one week after it.

- **a Custom Mon/Wed series lands on those weekdays only** — notrun (calendar.spec.js:2191)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, type a unique title such as “[e2e] custom …” and choose Personal → Set Start Date to the next Monday that is at least two days away, turn All Day off and pick Start Time 7:30 AM → Set Repeat to Custom and tick Mon and Wed → Click Create Event and check that it is saved as a custom repeat on Monday and Wednesday → Switch to Day view on that Monday and check that the event is shown → Go to the Tuesday and check that the event is not shown → Go to the Wednesday and check that the event is shown → Open the event, click Delete, choose “All events in this series”, click Delete and check that a success message appears
  - Expected: A custom Monday/Wednesday series appears on Monday and Wednesday but not on the Tuesday between them.

- **edit "This event only" — diff payload, detail loads, panel shows the edit (F9, I12, I13)** — notrun (calendar.spec.js:2231)
  - Where: Home → Calendar tab → Day view · account: iPhone XRR
  - Setup: Creates a Personal daily series “[e2e] daily one …” at 6:30 AM, from two days ahead until five days ahead (Number of Days 3), through the New Event form, and checks that it is saved as daily.
  - Steps: Switch to Day view three days ahead, open the event and check that the detail says Recurring → Click Edit Event, choose “This event only” and click Continue → Check that the dialog is titled “Edit this event” and has no Repeat field → Add “ ONE” to the title, click Update Event and check that only that day is saved (new title, no repeat rule or start time sent) → Check that the “… ONE” event is on the grid and its detail opens with that title, then press Esc → Switch to Day view four days ahead and check that the original title is shown and “… ONE” is not → Open Events menu → Created Events → Personal, search the title and click “View full event details” on the card → Check that the occurrences panel shows “Edited occurrence” and a “View Full Details” button, then click Close and close the drawer → Switch to Day view two days ahead, open the event, click Delete, choose “All events in this series” and click Delete
  - Expected: Editing one occurrence renames only that day: its detail loads, the other days keep the original title, and the series panel in Created Events marks it as an edited occurrence.

- **edit "This and following" splits the series** — notrun (calendar.spec.js:2300)
  - Where: Home → Calendar tab → Day view · account: iPhone XRR
  - Setup: Creates a Personal daily series “[e2e] daily split …” at 5:30 AM, from two days ahead until five days ahead (Number of Days 3), through the New Event form.
  - Steps: Switch to Day view four days ahead, open the event and click Edit Event → Choose “This and following events” and click Continue → Check that the dialog is titled “Edit this and following events” and has no Repeat or Repeat Until field → Add “ LATER” to the title, click Update Event and check that it is saved from that day onward with a start time and no repeat rule → Switch to Day view three days ahead and check that the original title is still shown → Switch to Day view five days ahead and check that “… LATER” is shown and the original title is not → Open “… LATER”, click Delete, choose “All events in this series” and click Delete → Switch to Day view two days ahead, open the original event and delete all events in its series the same way
  - Expected: “This and following” splits the series: days before the edited day keep the original title, and from that day on the events carry the new title.

- **edit "All events" can change the frequency** — notrun (calendar.spec.js:2352)
  - Where: Home → Calendar tab → Day view two days from today · account: iPhone XRR
  - Setup: Creates a Personal weekly series “[e2e] weekly to daily …” two days from today at 4:30 AM (default end: one week later), through the New Event form.
  - Steps: Switch to Day view two days ahead and open the event → Click Edit Event, choose “All events in this series” and click Continue → Check that the dialog is titled “Edit all events” → Set Repeat to Daily and press Ok on the notice → Click Update Event and check that the whole series is saved with a daily repeat → Switch to Day view three days ahead and check that the event now appears there → Open it, click Delete, choose “All events in this series” and click Delete
  - Expected: Editing all events can change a weekly series to daily, so it now also appears on the next day.

- **delete "This event only" (and Cancel sends nothing)** — notrun (calendar.spec.js:2382)
  - Where: Home → Calendar tab → Day view · account: iPhone XRR
  - Setup: Creates a Personal daily series “[e2e] daily del one …” at 3:30 AM, from two days ahead until four days ahead (Number of Days 2), through the New Event form.
  - Steps: Switch to Day view three days ahead, open the event and click Delete → Check that “Delete recurring event” opens with “This event only” selected, then click Cancel → Check that nothing was deleted → Open the event again, click Delete, choose “This event only” and click Delete → Check that only that day's occurrence (three days ahead) is cancelled and it no longer shows as a live event → Switch to Day view four days ahead and check that the event there is still live → Switch to Day view two days ahead, open the event, click Delete, choose “All events in this series” and click Delete
  - Expected: Cancel in the scope dialog deletes nothing, and “This event only” cancels just that day while the other days stay live.

- **delete "This and following" ends the series from that date** — notrun (calendar.spec.js:2441)
  - Where: Home → Calendar tab → Day view · account: iPhone XRR
  - Setup: Creates a Personal daily series “[e2e] daily del tail …” at 2:30 AM, from two days ahead until five days ahead (Number of Days 3), through the New Event form.
  - Steps: Switch to Day view four days ahead, open the event and click Delete → Check that “Delete recurring event” offers “This and following events (from <that date>)” → Choose that option and click Delete → Check that the delete succeeds for that day and every day after it → Switch to Day view five days ahead and check that the event is no longer live → Switch to Day view three days ahead and check that the event is still live → Switch to Day view two days ahead, open the event, click Delete, choose “All events in this series” and click Delete
  - Expected: “This and following events” cancels the series from the chosen day on, while earlier days stay live.

### Calendar — drawers

- **Received: Pending then Accepted fetch their status @smoke** — notrun (calendar.spec.js:2503)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Received Events” → Check that the drawer opens on the Pending tab and loads pending invitations → Check that it says “Manage invitations you’ve received” → Check that the “Other Date” filter is shown → Click the “Accepted” tab → Check that the accepted invitations are loaded → Press Escape to close the drawer
  - Expected: The Received Events drawer opens on Pending with its subtitle and date filter, and the Accepted tab loads the accepted invitations.

- **Received: the Expired tab hides the date filter (marks badges read)** — notrun (calendar.spec.js:2518)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Received Events” → Click the “Expired” tab → Check that the expired invitations are loaded → Check that the “Other Date” filter is gone → Press Escape to close the drawer
  - Expected: The Expired tab loads the expired invitations and shows no date filter.

- **Received search goes to the server and can come back empty @smoke** — notrun (calendar.spec.js:2531)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Received Events” → Type a nonsense word such as “zz-e2e-no-match-123” in “Search by event title, host or keyword...” → Check that the search is run on the server → Check that the drawer shows “No pending events” → Press Escape to close the drawer
  - Expected: The search reaches the server and a search with no match shows “No pending events”.

- **the date filter: Today, Other Date, Clear @smoke** — notrun (calendar.spec.js:2548)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Received Events” → Check that “Clear” is disabled → Click “Today” and check that the list reloads for today and “Clear” becomes enabled → Click “Clear” and check that the list reloads without a date and “Clear” is disabled again → Click “Select date”, type tomorrow’s date (e.g. “Nov 17, 2026”) and press Enter → Check that the list reloads for tomorrow and “Clear” is enabled → Press Escape to close the drawer
  - Expected: Today and a picked other date each reload the list for that day and enable Clear; Clear reloads the full list and disables itself.

- **the filter button next to the search opens filters @smoke** — notrun (calendar.spec.js:2582)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Received Events” → Click the filter (funnel) button next to the search box → Check that a filter menu or popup opens within 5 seconds → Press Escape to close the drawer
  - Expected: Clicking the filter button opens a filter menu. Known bug: the button does nothing when clicked.

- **Created: the three tabs, each with its empty state @smoke** — notrun (calendar.spec.js:2598)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Created Events” → Check that the drawer opens on “Invites Sent” and loads those events → Check that the “Other Date” filter is shown, type a nonsense word in “Search by event title...” and check for “No invites sent events” → Click the “Personal” tab and check that the personal events load → Check that “Other Date” is shown, search a nonsense word and check for “No personal events” → Click the “Expired” tab and check that the expired events load → Check that “Other Date” is gone, search a nonsense word and check for “No expired events” → Press Escape to close the drawer
  - Expected: Each of the three tabs loads its own list, Expired has no date filter, and a search with no match shows that tab’s own empty message.

- **Created → Personal pages with Load More @smoke** — notrun (calendar.spec.js:2628)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Created Events” → Click the “Personal” tab → Wait for the “Load More” button at the bottom of the list → Count the event cards → Click “Load More” → Check that the next page loads and there are more cards than before → Press Escape to close the drawer
  - Expected: Load More fetches the next page and adds cards to the Personal list.

- **Declined → Received lists my declines @smoke** — notrun (calendar.spec.js:2653)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Declined Events” → Check that the drawer opens on its Received tab and loads the invitations you declined → Type a nonsense word in “Search by event title...” → Check that the drawer shows “No events you declined” → Press Escape to close the drawer
  - Expected: Declined Events opens on the invitations you declined, and a search with no match shows “No events you declined”.

- **Declined → Created lists decliners (marks the badge read)** — notrun (calendar.spec.js:2669)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Declined Events” → Click the “Created” tab → Check that it loads your active events that someone declined → Type a nonsense word in “Search by event title...” → Check that the drawer shows “No events with decliners” → Press Escape to close the drawer
  - Expected: The Created tab lists your active events with decliners, and a search with no match shows “No events with decliners”.

- **the Join Events Call drawer lists today @smoke** — notrun (calendar.spec.js:2687)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the “Join Events Call” button in the calendar header → Check that the “Join Events Call” drawer opens and loads today’s call events → Check that it says “Call events scheduled for today, <today’s date>.” (e.g. “Nov 16, 2026”) → Type a nonsense word in “Search by event title...” → Check that the drawer shows “No call events for today” → Press Escape to close the drawer
  - Expected: The drawer loads today’s call events, names today’s date, and a search with no match shows “No call events for today”.

- **a call event today shows in Join Events Call with Join** — notrun (calendar.spec.js:2710)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: Creates a Members event “[e2e] join today <time>” for today, two hours from now on the hour, Call Type “Audio Call”, with one external guest e2e+<time>@example.com (the “Add User” question answered No).
  - Steps: Click the + button, type the title, choose “Members”, turn All Day off and set the start time two hours from now → Pick “Audio Call” as Call Type, add the external guest under “Add External Guests” and press “Create Event” → Click the “Join Events Call” button in the calendar header → Type the event title in “Search by event title...” → Check that the event is listed → Check that a “Join” button is shown for it (do not click it) → Press Escape to close the drawer
  - Expected: A call event created for later today appears in Join Events Call with a Join button.

- **a drawer resets its tab and search when reopened @smoke** — notrun (calendar.spec.js:2741)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Received Events” → Click the “Accepted” tab → Type “something” in “Search by event title, host or keyword...” → Press Escape to close the drawer → Open “Received Events” again from the Events menu → Check that it opens on Pending and loads pending invitations → Check that the search box is empty
  - Expected: A reopened Received Events drawer starts again on Pending with an empty search.

### Calendar — calls

- **Line 1 Join opens the meet lobby (never joins)** — notrun (calendar.spec.js:2766)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: Creates a Members event “[e2e] line 1 <time>” for tomorrow at 10:30 AM, Call Type “Audio Call”, with one external guest e2e+<time>@example.com (the “Add User” question answered No).
  - Steps: Create the event through the + button and “Create Event”, leaving Call Line at its default → Check that the event was saved on Line 1 → Switch to Day view and go to tomorrow → Click the event to open Event Details → Click “Join” → Check that the in-page call window (the meeting lobby, with Minimize and Maximize buttons) opens → Check that Event Details closes → Reload the calendar to leave the lobby without joining
  - Expected: Join on a Line 1 event opens the meeting lobby and closes the event detail; the test never joins the call.

### Calendar — theme

- **the rail toggle themes the calendar and its portals @smoke** — notrun (calendar.spec.js:2812)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Note whether the calendar is light or dark → Click the sun/moon theme button at the end of Home’s unit tab rail → Check that the calendar switches to the other theme (in dark, a dark navy background) → Open the Events menu, choose “Created Events” and check that the drawer uses the new theme → Press Escape to close the drawer → Click the + button and check that the New Event form uses the new theme → Press “Cancel” on the form
  - Expected: The rail’s theme button switches the calendar, its drawers and its form to the other theme.

- **static warnings follow the dark theme @smoke** — notrun (calendar.spec.js:2845)
  - Where: Home → Calendar tab, Month view · account: iPhone XRR
  - Steps: Switch to the dark theme with the sun/moon button on Home’s unit tab rail, if it is not dark already → Click a past day in the month (go to the previous month if this month shows no past day) → Check that the warning “You cannot add events on previous date.” appears → Check that the warning box is not white → Press “Ok”
  - Expected: The past-date warning uses the dark theme. Known bug: the warning (like the other small warning and confirm boxes) stays white in dark.

### Calendar — other entry points

- **/calendar opens calendarV3 with its own theme toggle @smoke** — notrun (calendar.spec.js:2889)
  - Where: Home → Calendar tab, then the stand-alone Calendar window (type the app address followed by /calendar) · account: iPhone XRR
  - Steps: Click the “Bulletin” tab on Home so Home’s calendar closes → Check that no calendar is on screen → Open the stand-alone Calendar page by its address ending in /calendar → Check that exactly one calendar is on screen and its grid shows → Check that this calendar has its own “Toggle theme” button
  - Expected: The stand-alone Calendar opens the same new calendar, with its own Toggle theme button.

- **Email → Calendar shows calendarV3 @smoke** — notrun (calendar.spec.js:2911)
  - Where: Home → Calendar tab, then Email · account: iPhone XRR
  - Steps: Click the “Bulletin” tab on Home so Home’s calendar closes → Check that no calendar is on screen → Open Email → Click the Calendar button in Email’s navigation strip → Check that a calendar opens inside Email with its grid → Check that it has a “Toggle theme” button
  - Expected: Email’s Calendar button shows the new calendar inside Email, with its own Toggle theme button.

- **production /calendar is the legacy newCalendar @smoke** — notrun (calendar.spec.js:2934)
  - Where: The live (production) site, stand-alone Calendar page (app address followed by /calendar) · account: iPhone XRR
  - Steps: Open the stand-alone Calendar page by its address ending in /calendar → Check that the older calendar design opens → Check that its “Month” view button is shown
  - Expected: On the live site the stand-alone Calendar is still the older calendar, with its Month button.

### Calendar — two users

- **B invites A: live badge + drawer, A accepts (M1, A8, J13, L8)** — notrun (calendar.spec.js:3007)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] invite A <time>” three days ahead at 9:30 AM, Meet in Person, inviting iPhone XRR.
  - Steps: As iPhone XRR, note the Events badge and the Calendar tab’s unread count, then open “Received Events” and leave it open → As Lava Device, create the event and invite iPhone XRR → As iPhone XRR, check that the event appears in the open Pending list without a reload, then close the drawer → Check that the Events badge and the Calendar tab’s unread count both go up → Go to Day view three days ahead and open the event: check “Pending invitation”, a Decline button and no Join button → Click “Accept”, check the “Accept Invitation” dialog asks “Are you sure you want to accept this invitation?”, and press “Accept” → Check the success message and that the whole event is accepted → Open the event again: check “You accepted this event” and no Accept button → Open Received Events → Accepted, search the title and check it is listed → As Lava Device, open Created Events → Invites Sent, search the title, click “See invitees” and check “1 Accepted”
  - Expected: The invite reaches iPhone XRR live (open drawer, Events badge, Calendar tab count); accepting it marks the event accepted for iPhone XRR and Lava Device sees “1 Accepted”.

- **A declines with a reason; B sees it (C4, J9, M3)** — notrun (calendar.spec.js:3087)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] decline A <time>” four days ahead at 8:30 AM, Meet in Person, inviting iPhone XRR.
  - Steps: As iPhone XRR, go to Day view four days ahead and open the event → Click “Decline”, type a reason such as “e2e reason 123” in “Let the organizer know why you can't attend...” and press “Decline” → Check that the decline is saved with the reason for the whole event → Check that the event on the grid turns to Declined (or disappears) → Open Declined Events, search the title and check it is listed, then close the drawer → As Lava Device, open the Events menu and check that “Declined Events” carries a badge → Open Declined Events → Created, search the title and click “Show decliners (1)” → Check that it shows iPhone XRR’s name and the reason in quotes → Close the drawer and check that the Declined Events badge is gone
  - Expected: iPhone XRR’s decline with a reason is saved and shown as declined; Lava Device gets a Declined badge, reads the decliner and the reason, and the badge clears after opening Created.

- **B requests a decline reason (sends only with E2E_REAL_SEND=1)** — notrun (calendar.spec.js:3141)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] reason request <time>” five days ahead at 7:30 AM, Meet in Person, inviting iPhone XRR; iPhone XRR declines it from Event Details without a reason.
  - Steps: As iPhone XRR, go to Day view five days ahead, open the event, click “Decline” and press “Decline” with no reason → As Lava Device, open Declined Events → Created, search the title and click “Show decliners (1)” → Click “Request Reason” and check that the “Request decline reason” dialog opens → Check that “Send Request” is disabled while the message is empty → Type only spaces and check that “Send Request” stays disabled → Type “e2e: why not?” and check that “Send Request” becomes enabled → Press “Cancel” and check that the dialog closes (only a run set to send real messages presses “Send Request” and checks “Message sent”) → Close the drawer
  - Expected: Send Request is enabled only once real text is typed. Normally the dialog is cancelled; when real sending is switched on, the request is sent and “Message sent” appears.

- **the card-level Accept sends no scope** — notrun (calendar.spec.js:3190)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] card accept <time>” five days ahead at 6:30 PM, Meet in Person, inviting iPhone XRR.
  - Steps: As iPhone XRR, open the Events menu and choose “Received Events” → Search the event title and check that it is listed → Click “Accept” on the event’s card → Check the “Accept Invitation” dialog asks “Are you sure you want to accept this invitation?” → Press “Accept” → Check that the accept succeeds and is sent without any choice of which occurrences it applies to → Press Escape to close the drawer
  - Expected: Accepting from the drawer card succeeds and sends a plain accept, with no “apply to” choice or occurrence date.

- **a scoped decline of a recurring invite (L5 admin occurrence RSVP)** — notrun (calendar.spec.js:3225)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] recurring invite <time>” starting two days ahead at 1:30 PM, Meet in Person, Repeat Daily with a count of 2, inviting iPhone XRR.
  - Steps: As iPhone XRR, go to Day view three days ahead and open the event; check that it says Recurring → Click “Decline” and check that the dialog shows “Apply to” with “This event only” selected → Choose “This and following events” and press “Decline” → Check that the decline is saved as “this and following” starting from that day → Go to Day view two days ahead and check that the earlier occurrence is still a normal, undeclined event → Go to Day view four days ahead and check that the event is shown as declined or not shown → Open Received Events, search the title and click the card to see all occurrences → Check that the occurrence list offers Accept or Decline on its rows
  - Expected: Declining “This and following events” from the second day leaves the first day untouched, and the occurrence list lets the invitee answer each occurrence. Known bug: an admin invitee sees Edit and Delete instead of Accept and Decline on the occurrence rows.

- **B edits and cancels: A updates live without a spinner** — notrun (calendar.spec.js:3296)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] realtime <time>” six days ahead at 10:00 AM, Meet in Person, inviting iPhone XRR.
  - Steps: As iPhone XRR, go to Day view six days ahead and check that the event is shown; keep watching this screen → As Lava Device, go to Day view six days ahead, open the event and click “Edit Event” → Rename it to “<title> B-EDIT” and press “Update Event” → As iPhone XRR, check that the renamed event appears without a reload → Check that the calendar never showed its loading spinner while it updated → As Lava Device, open the renamed event, click “Delete” and confirm “Delete” → As iPhone XRR, check that the event turns to “Cancelled:” (or disappears) without a reload
  - Expected: Lava Device’s rename and cancel reach iPhone XRR’s grid live, and the live refresh shows no loading spinner.

- **admin A edits an event B created** — notrun (calendar.spec.js:3356)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] admin edits <time>” six days ahead at 3:30 PM, Meet in Person, inviting iPhone XRR.
  - Steps: As iPhone XRR, go to Day view six days ahead and open the event → Check that “Edit Event” and “Delete” are shown → Click “Edit Event”, rename it to “<title> BY-ADMIN” and press “Update Event” → As Lava Device, go to Day view six days ahead → Check that the renamed event is shown
  - Expected: As a project admin, iPhone XRR can edit an event Lava Device created, and Lava Device sees the new title.

- **a non-admin invitee: no Edit/Delete, no All Departments** — notrun (calendar.spec.js:3392)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR creates a Members event “[e2e] non admin <time>” six days ahead at 5:30 PM, Meet in Person, inviting Lava Device.
  - Steps: Check that Lava Device is not a project admin → As iPhone XRR, create the event and invite Lava Device → As Lava Device, go to Day view six days ahead and open the event → Check that there is no “Edit Event” and no “Delete”, and that “Accept” is shown → Press Escape, click the + button and choose “Members” → Click “Add Invitees” and check that the “Select Invitees” dialog has no “All Departments” option → Press “Cancel” in the dialog and “Cancel” on the form
  - Expected: A non-admin invitee can only answer another user’s event (no Edit or Delete) and cannot invite All Departments.

### Calendar — edge: text

- **unicode, emoji, RTL and HTML render as plain text** — notrun (calendar.spec.js:3464)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: Creates a Personal event seven days ahead at 8:15 AM titled “[e2e] ünïcødé 漢字 😀 שלום <b>x</b> <time>”, with a two-line description: line 1 holds a <script> tag, line 2 an <img> tag with an error handler and an emoji.
  - Steps: Click the + button, type the title, choose “Personal”, set the date seven days ahead, turn All Day off, set 8:15 AM, type the description and press “Create Event” → Check that the title and description are saved exactly as typed → Go to Day view seven days ahead and open the event → Check that the heading shows the title exactly as typed, with “<b>x</b>” as text and nothing in bold → Check that a DESCRIPTION section shows the <script> tag as plain text, with no image and the line break kept → Check that none of the script in the title or description ran → Press Escape, open Created Events → Personal and search “漢字” → Check that the event is found
  - Expected: Unicode, emoji, right-to-left text and HTML are saved as typed and shown as plain text (no bold, no image, no script runs), and the server search finds the event by its Chinese characters.

- **a 3-character title passes the client (intercepted)** — notrun (calendar.spec.js:3508)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test blocks every calendar save: each one is answered with a failure and nothing reaches the server.
  - Steps: Click the + button to open the New Event form → Type “abc” as the title → Choose “Personal”, set the date two days ahead and turn All Day on → Press “Create Event” → Check that exactly one save is sent, with the title “abc” → Check that no “at least 3 characters” error is shown → Press “Cancel” on the form
  - Expected: A 3-character title passes the form’s checks and is sent to the server.

- **there is no client length limit: 1000 characters are sent (intercepted)** — notrun (calendar.spec.js:3533)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test blocks every calendar save: each one is answered with a failure and nothing reaches the server.
  - Steps: Click the + button to open the New Event form → Paste a 1000-character title (“[e2e] ” followed by 994 x’s) → Choose “Personal”, set the date two days ahead and turn All Day on → Press “Create Event” → Check that exactly one save is sent, with all 1000 characters of the title → Press “Cancel” on the form
  - Expected: The form has no title length limit: all 1000 characters are sent. This is recorded as an observation; whether the server accepts it is a manual check.

- **a 150-character title round-trips and stays whole in the detail** — notrun (calendar.spec.js:3564)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: Creates a Personal event seven days ahead at 9:15 AM with a 150-character title (“[e2e] long <time> ” followed by y’s).
  - Steps: Click the + button, paste the title, choose “Personal”, set the date seven days ahead, turn All Day off, set 9:15 AM and press “Create Event” → Go to Day view seven days ahead → Check that the event is shown and its screen-reader label ends with the full title → Open the event → Check that the Event Details heading holds the full 150-character title (it may be cut short on screen with “…”, but not in the text) → Click “Delete” and confirm “Delete”
  - Expected: A 150-character title is saved and comes back whole, both on the grid and in Event Details.

### Calendar — edge: times & dates

- **exactly 15 minutes is accepted; end times step by 5 minutes (intercepted)** — notrun (calendar.spec.js:3603)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test blocks every calendar save: each one is answered with a failure and nothing reaches the server.
  - Steps: Click the + button, type a title “[e2e] fifteen <time>”, choose “Personal” and set the date two days ahead → Turn All Day off and set Start Time to 10:00 AM → Open End Time and type “10:1” → Check that “10:14 AM” is not offered and “10:10 AM” is → Pick “10:15 AM” → Press “Create Event” → Check that one save is sent, starting at 10:00 AM (India time) two days ahead and lasting exactly 15 minutes → Press “Cancel” on the form
  - Expected: End times are offered in 5-minute steps, and an event of exactly 15 minutes is accepted and sent.

- **an end before the start means the next day (intercepted)** — notrun (calendar.spec.js:3637)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test blocks every calendar save: each one is answered with a failure and nothing reaches the server.
  - Steps: Click the + button, type a title “[e2e] end before start <time>”, choose “Personal” and set the date two days ahead → Turn All Day off and set Start Time 10:00 AM and End Time 9:00 AM → Check that End Date moves to the next day (three days ahead) → Press “Create Event” → Check that one save is sent, from 10:00 AM two days ahead to 9:00 AM three days ahead (India time) → Press “Cancel” on the form
  - Expected: An end time before the start is read as the next morning: End Date moves on a day and the event is sent as an overnight event. This is by design; the form gives no warning (recorded as an observation).

- **the last and the first slot of a day (intercepted)** — notrun (calendar.spec.js:3669)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test blocks every calendar save: each one is answered with a failure and nothing reaches the server.
  - Steps: Click the + button, type a title “[e2e] day edges <time>”, choose “Personal”, set the date two days ahead and turn All Day off → Set Start Time to 11:55 PM → Check that End Time fills in as 12:55 AM and End Date moves to the next day → Press “Create Event” and check that it is sent as 11:55 PM to 12:55 AM the next day → Set Start Time to 12:00 AM (the form stays open because the save was refused) → Check that End Time fills in as 1:00 AM and End Date is back on the start day → Press “Create Event” and check that it is sent as 12:00 AM to 1:00 AM on the same day → Press “Cancel” on the form
  - Expected: The last start slot (11:55 PM) ends at 12:55 AM the next day, and the first slot (12:00 AM) ends at 1:00 AM the same day; both are sent with those times.

- **half- and quarter-hour zones convert exactly (intercepted)** — notrun (calendar.spec.js:3706)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test blocks every calendar save: each one is answered with a failure and nothing reaches the server.
  - Steps: Click the + button, type a title “[e2e] zones <time>”, choose “Personal” and set the date three days ahead → Turn All Day off and set 10:00 AM to 11:00 AM → In Timezone, search “Kathmandu” and pick it; check Start Time and Start Date show the same moment in Kathmandu time (e.g. 10:15 AM) → Search “Chatham” and pick it; check the times and date show the same moment in Chatham time → Search “St_Johns” (or “St. John's”, “Newfoundland”) and pick it; check the times and date show the same moment in St John’s time → Search “Kolkata” and pick it; check Start Time is back to 10:00 AM on the original date → Press “Create Event” → Check that the save is sent for 10:00 AM India time three days ahead, in the Kolkata zone → Press “Cancel” on the form
  - Expected: Switching to zones with 45- and 30-minute offsets shows the exact local time of the same moment, and switching back to Kolkata restores 10:00 AM, which is what is sent.

- **an overnight event across the year boundary sits on Dec 31 and Jan 1** — notrun (calendar.spec.js:3780)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: Creates a Personal event “[e2e] new year <time>” on the coming Dec 31 (next year’s if this year’s is less than two days away), 11:00 PM to 12:30 AM.
  - Steps: Click the + button, type the title, choose “Personal” and set Start Date to Dec 31 → Turn All Day off and set 11:00 PM to 12:30 AM → Check that End Date shows Jan 1 of the next year → Press “Create Event” and check that it is saved from 11:00 PM Dec 31 to 12:30 AM Jan 1 (India time) → Switch to Month view and go to December; check the event is in the Dec 31 cell → Go to January; check the event is in the Jan 1 cell
  - Expected: An overnight event across New Year is saved with the right times and shows on both Dec 31 and Jan 1.

- **Feb 29: the form takes the leap day and a Yearly series clamps to Feb 28 @smoke** — notrun (calendar.spec.js:3814)
  - Where: Home → Calendar tab, Month view · account: iPhone XRR
  - Steps: Switch to Month view and press Next until the month of the next Feb 29 (e.g. February 2028) → Click the Feb 29 cell → Check that the New Event form opens with Start Date Feb 29 → Choose “Personal” → Choose Repeat “Yearly” and press Ok on the heads-up → Check that Repeat Until shows Feb 28 of the following year → Press “Cancel” on the form
  - Expected: The form accepts Feb 29 as the start, and a Yearly series from it ends on Feb 28 of the following (non-leap) year.

- **repeat counts at 1, at each maximum, above it and at 0 @smoke** — notrun (calendar.spec.js:3835)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, choose “Personal”, set the date two days ahead and turn All Day on → Choose Repeat “Daily” (press Ok on the heads-up) → Type 1 in “Number of Days” next to Repeat Until and click away; check Repeat Until is one day after the start → Type 365 and check Repeat Until is 365 days after the start → Type 366 and check the count drops back to 365 with the same Repeat Until → Type 0 and check the count becomes 1 with Repeat Until one day after the start → Repeat the same four checks for “Weekly” (Number of Weeks, maximum 52), “Monthly” (Number of Months, 12) and “Yearly” (Number of Years, 5) → Press “Cancel” on the form
  - Expected: For each repeat type, the “Number of …” box and Repeat Until stay in step at 1 and at the maximum, a number above the maximum drops back to it, and 0 becomes 1.

- **Repeat Until = start + 1 day gives one more occurrence (intercepted)** — notrun (calendar.spec.js:3874)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test blocks every calendar save: each one is answered with a failure and nothing reaches the server.
  - Steps: Click the + button, type a title “[e2e] until plus one <time>”, choose “Personal”, set the date two days ahead and turn All Day on → Choose Repeat “Daily” (press Ok on the heads-up) and set Repeat Until to the day after the start → Check that “Number of Days” shows 1 and press “Create Event” → Choose Repeat “Weekly” (press Ok) and set Repeat Until to the day after the start again → Check that “Number of Weeks” shows 1 and press “Create Event” → Check that two saves were sent, a Daily and a Weekly series, both ending at 23:59:59 India time on the Repeat Until day → Press “Cancel” on the form
  - Expected: A Repeat Until one day after the start gives a count of 1, and both the Daily and the Weekly series are sent to run until the end of that day.

- **"Does not repeat" removes the repeat fields; All Day hides the times @smoke** — notrun (calendar.spec.js:3911)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, choose “Personal” and set the date two days ahead → Turn All Day off and check that Start Time and End Time are shown → Turn All Day on and check that Start Time and End Time disappear and End Date equals Start Date → Choose Repeat “Weekly” (press Ok on the heads-up) and check that Repeat Until appears → Choose Repeat “Custom” and check that “Select Days” appears → Choose Repeat “Does not repeat” and check that Repeat Until, the “Number of …” box and Select Days all disappear → Press “Cancel” on the form
  - Expected: All Day hides the time fields, and “Does not repeat” removes every repeat field.

- **a Monthly series from the 31st clamps to the shorter month @smoke** — notrun (calendar.spec.js:3938)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Find the next 31st whose following month is shorter (e.g. Jan 31, followed by February) → Click the + button, choose “Personal” and set Start Date to that 31st → Turn All Day on → Choose Repeat “Monthly” and press Ok on the heads-up → Check that Repeat Until shows the last day of the following month (e.g. Feb 28) → Press “Cancel” on the form
  - Expected: A Monthly series starting on the 31st ends on the last day of the shorter next month.

### Calendar — edge: form states

- **the Repeat and Reminder option lists are exact @smoke** — notrun (calendar.spec.js:3970)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button and choose “Personal” → Open the Repeat list → Check that it offers exactly, in order: Does not repeat, Daily, Weekly, Monthly, Yearly, Custom → Click the title box to close the list and open the Reminder list → Check that it offers exactly, in order: None, 5 minutes before, 10 minutes before, 15 minutes before, 30 minutes before, 1 hour before → Click the title box and press “Cancel” on the form
  - Expected: The Repeat and Reminder lists contain exactly those options in that order.

- **a timezone search with no match says so @smoke** — notrun (calendar.spec.js:4000)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button to open the New Event form → Open the Timezone list → Type “zz-no-such-zone” → Check that the list says “No data” → Click the title box and press “Cancel” on the form
  - Expected: A timezone search with no match shows “No data”.

- **external guests: Enter adds, inline edit, invalid edit refused @smoke** — notrun (calendar.spec.js:4015)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, choose “Members” and click “Add External Guests” → Type an address such as e2e+123@example.com and press Enter; answer “No” to the “Add User” question → Check that the address is listed → Click “Edit” on it, change it to “broken@” and click “Save” → Check that the error “Please enter a valid email address” appears → Change it to another address such as e2e+123-b@example.com and press Enter → Check that the new address replaced the old one → Press “Done (1)” and check that the form shows “1 external guest” → Click “Clear” in the External Guests field and check that “1 external guest” is gone → Press “Cancel” on the form
  - Expected: Enter adds a guest, an invalid edit is refused with “Please enter a valid email address”, a valid edit replaces the address, and Clear empties the guest list. Nothing is saved.

- **a department pick fills the invitees, Clear empties them @smoke** — notrun (calendar.spec.js:4054)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button, choose “Members” and click “Add Invitees” → In “Select Invitees”, open the “Departments” tab → Click the name of the first department → Check that “Selected” appears and note the number on the “Done (N)” button → Press “Done (N)” → Check that the Invitees field says “N people invited” (or “1 person invited”) → Click “Clear” in the Invitees field and check that the “invited” text is gone → Press “Cancel” on the form
  - Expected: Picking a department fills the invitees with its members, and Clear empties them again. Nothing is saved.

- **Create shows loading and a double click sends one request (intercepted)** — notrun (calendar.spec.js:4097)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test blocks every calendar save: each one is held for 3 seconds and then answered with a failure; nothing reaches the server.
  - Steps: Click the + button, type a title “[e2e] double click <time>”, choose “Personal”, set the date two days ahead and turn All Day on → Press “Create Event” → Check that the button shows a loading spinner at once → Click “Create Event” again while it is loading → Check that an error message appears when the save is refused → Check that only one save was sent and the button is no longer loading → Press “Cancel” on the form
  - Expected: Create Event shows a loading state while it saves, and a second click during that time sends nothing more.

- **Escape closes the form and discards it @smoke** — notrun (calendar.spec.js:4125)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Click the + button to open the New Event form → Type “escape me” as the title → Press Escape → Check that the form closes → Click the + button again → Check that the title is empty → Press “Cancel” on the form
  - Expected: Escape closes the New Event form and what was typed is thrown away.

- **create failures (HTTP 500, network) keep the form open (intercepted)** — notrun (calendar.spec.js:4138)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Setup: The test makes the create request fail: first with a server error (HTTP 500), then with a network failure.
  - Steps: With saves failing with a server error, click the + button → Type a title “[e2e] create http500 <time>”, choose “Personal”, set the date two days ahead and turn All Day on → Press “Create Event” → Check that an error message appears, the form stays open and the title is still filled in → Press “Cancel” on the form → With saves failing as a network failure, repeat the same steps with the title “[e2e] create abort <time>”
  - Expected: When the save fails with a server error or a network failure, an error message appears and the form stays open with what was typed.

### Calendar — edge: error & loading states

- **the grid: failures, slow load and recovery @smoke** — notrun (calendar.spec.js:4226)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Make the calendar’s event load fail with a “failed” reply from the server → Click Next period → Check that the loading spinner goes away and an error message (such as “failed”, “error” or “try again”) is shown → Let the load work again, click “Today” and check that the events load normally → Repeat the same three checks with a server error (HTTP 500) and with a network failure → Make the event load slow (held 4 seconds) and click Next period → Check that the loading spinner appears and then goes away
  - Expected: Each failed load shows an error and never leaves the spinner stuck; the next normal load works; a slow load shows the spinner and then the events. Known bug: a failed load looks like an empty calendar, with no error message.

- **Received drawer: failures, slow load and recovery @smoke** — notrun (calendar.spec.js:4258)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Make the received-invitations list fail with a “failed” reply from the server → Open the Events menu and choose “Received Events” → Check that, once loading stops, the drawer shows an error message (such as “failed”, “error” or “try again”), then close it → Repeat with a server error (HTTP 500) and with a network failure → Let the list load normally, open Received Events and check that it loads, then close it → Make the list slow (held 4 seconds), open Received Events → Check that a loading spinner shows and then the list appears, then close it
  - Expected: Each failed load shows an error in the drawer; the next normal open loads the list; a slow load shows a spinner and then the list. Known bug: a failed load looks like an empty list (“No pending events”), with no error.

- **Created drawer: failures, slow load and recovery @smoke** — notrun (calendar.spec.js:4270)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Make the created-events list fail with a “failed” reply from the server → Open the Events menu and choose “Created Events” → Check that, once loading stops, the drawer shows an error message (such as “failed”, “error” or “try again”), then close it → Repeat with a server error (HTTP 500) and with a network failure → Let the list load normally, open Created Events and check that it loads, then close it → Make the list slow (held 4 seconds), open Created Events → Check that a loading spinner shows and then the list appears, then close it
  - Expected: Each failed load shows an error in the drawer; the next normal open loads the list; a slow load shows a spinner and then the list. Known bug: a failed load looks like an empty list, with no error.

- **Declined → Received: failures, slow load and recovery @smoke** — notrun (calendar.spec.js:4282)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Make the list of invitations you declined fail with a “failed” reply from the server → Open the Events menu and choose “Declined Events” (it opens on its Received tab) → Check that, once loading stops, the drawer shows an error message (such as “failed”, “error” or “try again”), then close it → Repeat with a server error (HTTP 500) and with a network failure → Let the list load normally, open Declined Events and check that it loads, then close it → Make the list slow (held 4 seconds), open Declined Events → Check that a loading spinner shows and then the list appears, then close it
  - Expected: Each failed load shows an error in the drawer; the next normal open loads the list; a slow load shows a spinner and then the list. Known bug: a failed load looks like an empty list, with no error.

- **Declined → Created: failures, slow load and recovery (marks badge read)** — notrun (calendar.spec.js:4294)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Make the list of your events with decliners fail with a “failed” reply from the server → Open the Events menu, choose “Declined Events” and click the “Created” tab → Check that, once loading stops, the drawer shows an error message (such as “failed”, “error” or “try again”), then close it → Repeat with a server error (HTTP 500) and with a network failure → Let the list load normally, open Declined Events → Created and check that it loads, then close it → Make the list slow (held 4 seconds), open Declined Events → Created → Check that a loading spinner shows and then the list appears, then close it
  - Expected: Each failed load shows an error in the drawer; the next normal open loads the list; a slow load shows a spinner and then the list. Known bug: a failed load looks like an empty list, with no error.

- **Join Events Call drawer: failures, slow load and recovery @smoke** — notrun (calendar.spec.js:4311)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Make today’s call-events list fail with a “failed” reply from the server → Click the “Join Events Call” button in the calendar header → Check that, once loading stops, the drawer shows an error message (such as “failed”, “error” or “try again”), then close it → Repeat with a server error (HTTP 500) and with a network failure → Let the list load normally, open Join Events Call and check that it loads, then close it → Make the list slow (held 4 seconds), open Join Events Call → Check that a loading spinner shows and then the list appears, then close it
  - Expected: Each failed load shows an error in the drawer; the next normal open loads the list; a slow load shows a spinner and then the list. Known bug: a failed load looks like an empty list (“No call events for today”), with no error.

- **a detail opened from a drawer card with a failing fetch shows an error @smoke** — notrun (calendar.spec.js:4328)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu, choose “Created Events” and click the “Personal” tab → Make loading a single event’s details fail with a “failed” reply from the server → Click the first event card to view its full details → Check that the detail dialog opens → Check that it shows an error message (such as “failed”, “error” or “try again”) → Press Escape twice to close it
  - Expected: A detail opened from a drawer card whose load fails shows an error. Known bug: the detail keeps spinning forever.

- **the invitee list with a failing fetch shows an error @smoke** — notrun (calendar.spec.js:4371)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Open the Events menu and choose “Created Events” (it opens on Invites Sent) → Make the invitee list fail with a “failed” reply from the server → Click “See invitees” on the first card → Check that the invitee list opens → Check that it shows an error message (such as “failed”, “error” or “try again”) → Press Escape and close the drawer
  - Expected: An invitee list whose load fails shows an error. Known bug: it shows “No invitees”, as if the list were empty.

### Calendar — edge: layout & entry points

- **every drawer and modal is themed (dark) @smoke** — notrun (calendar.spec.js:4412)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Switch to the dark theme with the sun/moon button on Home’s unit tab rail → Open Received Events, Created Events and Declined Events from the Events menu in turn; check each drawer has a dark slate background, then close it → Click “Join Events Call”, check the drawer is dark slate and close it → Click the + button and check the New Event form is dark slate → Choose “Members”, click “Add Invitees”, check “Select Invitees” is dark slate and press “Cancel” → Click “Add External Guests”, check “External Guests” is dark slate and press “Cancel” → Click the Color button, choose “Select from preset colors”, check “Choose a Color” is dark slate and close it with × → Press “Cancel” on the form
  - Expected: In dark theme, all four drawers and the New Event, Select Invitees, External Guests and Choose a Color dialogs use the dark slate surface colour.

- **every drawer and modal is themed (light) @smoke** — notrun (calendar.spec.js:4412)
  - Where: Home → Calendar tab · account: iPhone XRR
  - Steps: Switch to the light theme with the sun/moon button on Home’s unit tab rail → Open Received Events, Created Events and Declined Events from the Events menu in turn; check each drawer has a white background, then close it → Click “Join Events Call”, check the drawer is white and close it → Click the + button and check the New Event form is white → Choose “Members”, click “Add Invitees”, check “Select Invitees” is white and press “Cancel” → Click “Add External Guests”, check “External Guests” is white and press “Cancel” → Click the Color button, choose “Select from preset colors”, check “Choose a Color” is white and close it with × → Press “Cancel” on the form
  - Expected: In light theme, all four drawers and the New Event, Select Invitees, External Guests and Choose a Color dialogs are white.

- **the calendar works in Classic view (restores Windowed)** — notrun (calendar.spec.js:4466)
  - Where: Home → Calendar tab, then the sidebar’s view switch · account: iPhone XRR
  - Steps: If the sidebar offers “Classic view”, click it and check that it now offers “Windowed view” → Open Home and click the “Calendar” tab → Check that exactly one calendar is shown and there are no tool windows → Check that the + button sits at the bottom-right corner of the screen → Open “Received Events” from the Events menu and close it → Click the + button and press “Cancel” on the New Event form
  - Expected: In Classic view the calendar shows once, without windows, with the + button in the bottom-right corner, and its drawer and form open normally.

- **at 768 px nothing scrolls sideways and the form/drawer fit @smoke** — notrun (calendar.spec.js:4508)
  - Where: Home → Calendar tab, in a browser window 768 pixels wide (900 high) · account: iPhone XRR
  - Steps: Make the browser window 768 pixels wide → Open Home → Calendar tab → Check that the page does not scroll sideways → Click the + button and check that the New Event form fits inside the window → Press “Cancel” on the form → Open “Received Events” and check that the drawer fits inside the window → Check that the page still does not scroll sideways → Press Escape to close the drawer
  - Expected: At 768 pixels wide the calendar has no sideways scroll and both the New Event form and the Received Events drawer fit on screen.

- **an event created in the /calendar window shows on the Home tab** — notrun (calendar.spec.js:4530)
  - Where: Home → Calendar tab, and the stand-alone Calendar window (type the app address followed by /calendar) · account: iPhone XRR
  - Setup: Creates a Personal event “[e2e] from /calendar <time>” eight days ahead at 9:45 AM, in the stand-alone Calendar window.
  - Steps: Click the “Bulletin” tab on Home so Home’s calendar closes → Open the stand-alone Calendar page by its address ending in /calendar → Click the + button, type the title, choose “Personal”, set the date eight days ahead, turn All Day off, set 9:45 AM and press “Create Event” → Close the Calendar window → Open Home and click the “Calendar” tab → Go to Day view eight days ahead → Check that the new event is shown
  - Expected: An event created in the stand-alone Calendar window shows on Home’s Calendar tab.

- **an event created in the Email calendar shows on the Home tab** — notrun (calendar.spec.js:4560)
  - Where: Home → Calendar tab, and Email → Calendar button · account: iPhone XRR
  - Setup: Creates a Personal event “[e2e] from email <time>” eight days ahead at 10:45 AM, in the calendar inside Email.
  - Steps: Click the “Bulletin” tab on Home so Home’s calendar closes → Open Email and click the Calendar button in its navigation strip → Check that the calendar shows inside Email → Click the + button, type the title, choose “Personal”, set the date eight days ahead, turn All Day off, set 10:45 AM and press “Create Event” → Close the Email window → Open Home, click the “Calendar” tab and go to Day view eight days ahead → Check that the new event is shown
  - Expected: An event created in Email’s calendar shows on Home’s Calendar tab.

### Calendar — edge: two users

- **RSVP cycle: failed accept, accept, decline with an HTML reason, accept again** — notrun (calendar.spec.js:4642)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] rsvp cycle <time>” three days ahead at 11:30 AM, Meet in Person, inviting iPhone XRR. For the first accept, the test makes iPhone XRR’s accept fail.
  - Steps: As iPhone XRR, go to Day view three days ahead, open the event, click “Accept” and confirm “Accept” while the accept is made to fail; check an error message, press “Cancel” and Escape → Open the event again and check it still says “Pending invitation” → Click “Accept”, confirm, reopen and check “You accepted this event” → Click “Decline”, type the reason “e2e <b>why</b> 😀 <time>” and press “Decline”; check the reason is saved as typed → Open Declined Events, search the title and open its card: check the heading “Declined: <title>”, “You declined this event”, and no Decline or Join button → As Lava Device, open Declined Events → Created, search the title, click “Show decliners (1)” and check the reason is shown in quotes as plain text, with nothing in bold → As iPhone XRR, click “Accept” in the declined event’s detail and confirm → Open Declined Events, search the title and check “No events you declined” → Open Received Events → Accepted, search the title and check it is listed
  - Expected: A failed accept leaves the invite pending; accept, decline with a reason and accept again each take effect; the HTML in the reason is shown as plain text; after the last accept the event leaves Declined and is under Accepted.

- **per-occurrence answers on a series stay per occurrence** — notrun (calendar.spec.js:4734)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] per occurrence <time>” starting two days ahead at 12:30 PM, Meet in Person, Repeat Daily with a count of 2, inviting iPhone XRR.
  - Steps: As iPhone XRR, go to Day view three days ahead, open the event, click “Accept”, choose “This event only” and confirm → Check the accept was sent for that day only, then reopen and check “You accepted this event” → Go to Day view four days ahead, open the event and check “Pending invitation” → Click “Decline”, choose “This event only” and press “Decline”; check the decline was sent for that day only → Go to Day view two days ahead, open the event and check it still says “Pending invitation” → As Lava Device, go to Day view three days ahead, open the event, click “See invitees” and check “1 Accepted” → Go to Day view four days ahead, open the event, click “See invitees” and check “1 Declined”
  - Expected: Answering one occurrence changes only that occurrence: one day accepted, one declined, the first still pending, and Lava Device sees the right answer on each day.

- **B edits and then cancels an event A has open** — notrun (calendar.spec.js:4793)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] open while edited <time>” four days ahead at 2:30 PM, Meet in Person, inviting iPhone XRR.
  - Steps: As iPhone XRR, go to Day view four days ahead, open the event and leave Event Details open → As Lava Device, go to Day view four days ahead, open the event and click “Edit Event” → Rename it to “<title> B-EDIT” and press “Update Event” → As iPhone XRR, check that the open Event Details heading changes to the new title → As Lava Device, open the renamed event, click “Delete” and confirm “Delete” → As iPhone XRR, check that the still-open Event Details says “This event has been cancelled”
  - Expected: An Event Details that is already open follows the organizer’s rename and cancel. Known bug: the open detail keeps the old title and stays live.

- **B removes A from the invitees while A is looking** — notrun (calendar.spec.js:4839)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates a Members event “[e2e] uninvited <time>” five days ahead at 3:45 PM, Meet in Person, inviting iPhone XRR and one external guest e2e+<time>@example.com (the “Add User” question answered No).
  - Steps: As iPhone XRR, go to Day view five days ahead and check the event is shown → Open Received Events, search the title, check it is listed and leave the drawer open → As Lava Device, go to Day view five days ahead, open the event and click “Edit Event” → Click “Clear” in the Invitees field (the external guest stays) and press “Update Event” → Check that no project member is left among the invitees → As iPhone XRR, check that the event disappears from the open drawer without a reload, then close the drawer → Go to Day view four days ahead and back to five days ahead → Check that the event is no longer on the grid
  - Expected: When the organizer removes iPhone XRR from the invitees, the event leaves iPhone XRR’s open Received list live and is gone from the grid.

- **the Events badge counts up by two and down after an RSVP** — notrun (calendar.spec.js:4885)
  - Where: Home → Calendar tab, for both accounts · account: iPhone XRR and Lava Device
  - Setup: Lava Device creates two Members events six days ahead, “[e2e] badge one <time>” at 8:45 AM and “[e2e] badge two <time>” at 9:45 AM, Meet in Person, both inviting iPhone XRR.
  - Steps: As iPhone XRR, note the number on the Events badge → As Lava Device, create both events inviting iPhone XRR → As iPhone XRR, check that the Events badge rises by at least 2 (within a minute) and note it → Go to Day view six days ahead, open “badge one”, click “Accept” and confirm → Check that the Events badge drops below the noted number (within a minute) → Open Received Events and check that the Pending tab shows a count → Press Escape to close the drawer
  - Expected: Two new invites raise the Events badge by two, and answering one lowers it; the Pending tab shows its count. The drop depends on the server recounting the badge.

### Calendar — edge: permissions (B demoted)

- **a non-admin B: no Edit/Delete on A’s event, no All Departments, own event manageable** — notrun (calendar.spec.js:4965)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Home → Calendar tab (both accounts) · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR turns off Lava Device’s Admin switch in User Management, then creates a Members event “[e2e] for non-admin B <time>” six days ahead at 11:15 AM, Meet in Person, inviting Lava Device.
  - Steps: As iPhone XRR, in User Management search “Lava Device” and turn its Admin switch off → Create the Members event inviting Lava Device → As Lava Device, go to Day view six days ahead and open the event: check there is no “Edit Event” and no “Delete”, and “Accept” is shown → Open Created Events, search the title and check “No invites sent events” → Click the + button, choose “Members”, click “Add Invitees” and check there is no “All Departments” option → Open the “Departments” tab and check it lists at most one department, then press “Cancel” twice → Create a Personal event “[e2e] non-admin own <time>” six days ahead at 12:15 PM → Open it, check “Edit Event” is shown, then click “Delete” and confirm
  - Expected: A non-admin cannot edit or delete someone else’s event, does not see it under Created, cannot invite All Departments and sees only their own department, but can still edit and delete their own event.

- **without the Calendar view right B has no Calendar tab, nor /calendar** — notrun (calendar.spec.js:5030)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Home (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR turns off Lava Device’s Admin switch, then in Lava Device’s posting rights turns off View in the Calendar row (noting the old View and Post values).
  - Steps: As iPhone XRR, in User Management search “Lava Device” and turn its Admin switch off → Open Lava Device’s posting rights, note the Calendar row’s Post and View switches, turn View off and close the dialog → As Lava Device, enter the project and open Home → Check that there is no “Calendar” tab → Open the stand-alone Calendar page by its address ending in /calendar and wait 15 seconds → Check that no calendar is shown
  - Expected: Without the Calendar view right, Lava Device has no Calendar tab and the stand-alone Calendar page is refused. Known bug: the stand-alone Calendar page still opens by its address.

- **without the Calendar post right B gets no create button** — notrun (calendar.spec.js:5062)
  - Where: Settings → Admin Settings → User Management (iPhone XRR); Home → Calendar tab (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR turns off Lava Device’s Admin switch, then in Lava Device’s posting rights sets the Calendar row to View on and Post off (noting the old values).
  - Steps: As iPhone XRR, in User Management search “Lava Device” and turn its Admin switch off → Open Lava Device’s posting rights, in the Calendar row turn View on and Post off, and close the dialog → As Lava Device, open Home → Calendar tab → Check that there is no + (create event) button
  - Expected: A member who may view but not post in Calendar gets no create button. Known bug: the + button and the New Event form are still available. The test never creates anything.

## Sheet bugs (36)

- **CAL-01** Deleting a single event leaves a struck-through "Cancelled:" row that still counts in the day badge — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Create a one-off event. 2. Delete it. 3. Look at that day's agenda and the day badge.
  - Expected: The event row is removed and the day badge goes down by one.
  - Actual: A struck-through "Cancelled: <title>" row stays and the day badge count is unchanged.

- **CAL-02** Deleting "All occurrences" leaves "Cancelled:" rows on every day of the series — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Create a Daily recurring event. 2. Delete it with "All occurrences". 3. Look at the days of the series and their badges.
  - Expected: All occurrences disappear and the day badges drop.
  - Actual: Every day keeps a "Cancelled:" row and the day badges are unchanged.

- **CAL-03** An event may get two rows in the device calendar — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Create an event in Zillit with a location and URL. 2. Open the iOS Calendar app on the same device.
  - Expected: One native row with the event's location, URL and footer.
  - Actual: An older event had two native rows, one of them a minimal copy without location, URL or footer.

- **CAL-04** A deleted event still shows as "Cancelled:" on the invitee's calendar and counts in the day badge — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Member A creates an event and invites member B. 2. A deletes the event. 3. On B's device look at that day in Home → Calendar.
  - Expected: The event is gone from B's agenda and B's day badge drops.
  - Actual: B still shows the event as a struck-through "Cancelled: <title>" row, and B's today badge counts it (2 for two deleted events).

- **CAL-05** No success toast after creating an event with an invitee — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Create a Members event with one invitee, a location and a reminder. 2. Tap Create.
  - Expected: "Calendar event has been successfully created." toast for about 2 s.
  - Actual: The form shows "Creating..." and closes; no toast appears (checked frame by frame for 3 s). A Personal event does show the toast.

- **CAL-06** "Show decliners" row has no name and offers "Request Reason" although the reason was saved — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. A invites B to an event. 2. B declines with a reason. 3. On A open Declined Events → Created → Show decliners.
  - Expected: B's name and the quoted reason.
  - Actual: A row with an empty avatar, no name, "Declined" and a "Request Reason" link. A's event detail sheet does show B as DECLINED with the reason, so the reason is stored.

- **CAL-07** After declining, the invitee's agenda row doesn't change to "Declined:" until a refetch — Low · Web: Not checked, Android: Not checked
  - Steps: 1. B declines an event A invited them to. 2. Look at B's agenda row for that event.
  - Expected: The row changes to "Declined: <title>" at once.
  - Actual: The row stays plain until the month is changed and changed back (refetch).

- **CAL-08** Deleting a Box Schedule event leaves a "Cancelled:" row on Home Calendar that counts in the day badge — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. In Production Diary / Box Schedule create an event for today (it mirrors to Home Calendar). 2. Delete it in Box Schedule. 3. Open Home → Calendar and pull to refresh.
  - Expected: The mirrored event disappears from Home Calendar and the day badge drops.
  - Actual: Home Calendar still shows a struck-through "Cancelled: <title>" row and today's badge counts it, also after pull-to-refresh and a refetch. It can't be removed from the UI.

- **CAL-09** Event details show the reminder "At time of event" when no reminder was set — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Create an event with the reminder left as None. 2. Open the event's detail sheet.
  - Expected: Reminder shows None (or is hidden).
  - Actual: The sheet says "At time of event".

- **CAL-10** Edit toast has a typo: "has beed successfully edited" — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Edit an event and save.
  - Expected: "...has been successfully edited."
  - Actual: "...has beed successfully edited."

- **CAL-11** A member who can't add sees "Add a schedule or event to get started" on an empty day — Low · Web: Not checked, Android: Not checked
  - Steps: 1. As a member without add rights open Production Diary / Box Schedule. 2. Open an empty future day.
  - Expected: An empty-state text without an add prompt.
  - Actual: The page says "Add a schedule or event to get started." although the member can't add.

- **CAL-12** Recurring events: Edit, Delete, Accept and Decline always act on the whole series - no 'This event / This and following / All' choice — High · Web: Not affected, Android: Not checked
  - Steps: 1. Create a weekly meeting with invitees. 2. Open its 3rd occurrence. 3. Delete -> Confirm (or Edit -> change the time -> Update).
  - Expected: A scope dialog: This event only / This and following / All events; only the chosen occurrences change.
  - Actual: No dialog. Delete sends DELETE calendar/{occurrence id}?delete_type=all with no occurrence_date, so the whole series is cancelled for everyone. Edit sends edit_type=all with the occurrence's own date as start_datetime, rewriting the series from that occurrence. Accept/Decline from the popover answer by the occurrence row id with start_datetime only.

- **CAL-13** Events saved by web/iOS as recurring (recurrence_rule.frequency, master_event_id) are read as one-offs — High · Web: Not checked, Android: Not checked
  - Steps: 1. Create a weekly meeting on the web. 2. On desktop drag one occurrence, or open See invitees.
  - Expected: Recognised as recurring: drag refused, invitee list per occurrence.
  - Actual: isRecurring only checks repeat_status / repeat_type, so a row with recurrence_rule.frequency=2 (or an exception row with master_event_id) is a one-off: dragging it is allowed and saves edit_type=all, rescheduling the whole series; the invitee list is asked without occurrence_date.

- **CAL-14** An event ending exactly at midnight is also shown on the next day — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Create Thursday 22:00-00:00 (or an event another client stored with an end at the next midnight). 2. Look at Friday in Month view and in the day panel.
  - Expected: Only on Thursday.
  - Actual: Also on Friday as a chip and counted in Friday's panel ('1 event'), because the overlap test is end >= dayStart.

- **CAL-15** Today (and fast paging) can leave the month blank: the visible month is not refetched — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Click Next four times, waiting for each month. 2. Click Today. (or: click Next six times quickly)
  - Expected: Today's events (or the landed month's events) are shown.
  - Actual: Today only moves the anchor and never loads; the last window was centred four months ahead, so today's month draws empty. Clicks made while a load is out are dropped by load()'s isBusy guard, so fast paging lands on an unfetched month.

- **CAL-16** Day view Next/Previous jump from the month anchor, not from the day on screen — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Month view: select 20 Nov (today is 5 Nov). 2. Switch to Day - it shows 20 Nov. 3. Click Next.
  - Expected: 21 Nov.
  - Actual: 6 Nov: shift() moves the anchor and clears the selection that Day view was showing. Week view meanwhile ignores the selection and shows the anchor's week.

- **CAL-17** An event that ended earlier today still offers Join Call, Edit/Delete and Accept/Decline — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. At 15:00 open an event that ran 09:00-10:00 today (as organiser, then as invitee).
  - Expected: Footer reads expired; no Join Call, Edit, Delete, Accept or Decline.
  - Actual: 'Finished' is measured against midnight today, so the event is treated as live all day: Join Call opens the room, the organiser can edit/delete, the invitee can accept. The invitations panel and day-panel Join Call use the same midnight clock.

- **CAL-18** A form or popover closed while its save/delete is in flight pops back open when the request fails — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Slow or failing network. 2. Fill an event -> Create, then immediately Cancel (or switch production). 3. The request fails. (Same for Delete -> Confirm, then close the popover.)
  - Expected: It stays closed; the failure is shown as a toast.
  - Actual: onError writes the captured form/detail back into state, so the closed form (even the previous production's, after a switch) reappears with the error.

- **CAL-19** Reload requests are dropped while a read is in flight, so another device's change can be missed — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open the calendar (or page months) so a read is running. 2. Another device creates an event during that read.
  - Expected: The new event appears once the reads finish.
  - Actual: The socket's reload hits load()'s 'already busy' early return; the in-flight read was answered before the change, so the new event stays missing until some later reload. Same for the reload after a local save that lands during a read.

- **CAL-20** Invitations: a new tab shows the previous tab's rows (with Accept/Decline) until - and if - its own load succeeds — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Invitations -> Pending lists invitations. 2. Go offline (or slow network) and click Accepted.
  - Expected: Accepted tab shows loading then its own rows, or an empty list with an error.
  - Actual: Pending rows, still with Accept/Decline buttons, stay under the Accepted tab while it loads, and permanently when the load fails.

- **CAL-21** Accepting from the event popover does not clear the invitation's unread badge — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Invitations button shows 1 unread. 2. Open that event from the grid -> Accept.
  - Expected: Badge drops, as when accepting in the Invitations panel.
  - Actual: The popover's accept path never calls badges.readAnswered (only the panel's sendAnswer does), and also skips markSelfAction, so the echo reloads twice.

- **CAL-22** A refused drag-reschedule snaps back silently - its error is wiped by the reload — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Drag an event and confirm Move. 2. The server refuses (offline, 4xx).
  - Expected: The event returns to its slot and an error toast says why.
  - Actual: onError sets the error and then calls load(), whose first act is error = null; the user sees the event jump back with no message.

- **CAL-23** Web's Created Events, Declined Events (request reason) and Join Events Call lists are missing — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Compare the calendar header menus with web.
  - Expected: Received / Created / Declined Events (with badge and 'request reason') / Join Events Call.
  - Actual: Desktop has only the Invitations (received) panel.

- **CAL-24** Week/Day view: clicking or dragging an empty time slot does not start a new event — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Week view. 2. Click (or drag over) Thursday 14:00-15:00.
  - Expected: New Event form prefilled with Thursday 14:00-15:00.
  - Actual: Only selects the day; nothing opens.

- **CAL-25** Project admins cannot edit or delete other users' events — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Sign in as an admin. 2. Open an event someone else created.
  - Expected: Edit and Delete offered (web ZL-18850).
  - Actual: Only the creator gets Edit/Delete.

- **CAL-26** Box Schedule-owned events can be edited, deleted and dragged from the Calendar — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Open an event created by Box Schedule. 2. Edit, Delete or drag it.
  - Expected: Blocked with the 'manage it from Box Schedule' notice (web ZL-19318).
  - Actual: Desktop never reads box_schedule_event; all three go ahead.

- **CAL-27** 'Today' is fixed at launch and never moves past midnight — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Leave the app open (or asleep) past midnight. 2. Wake, click Today.
  - Expected: Today highlight, greyed past days and the new-event default move to the new date.
  - Actual: state.today is only set in the constructor; Reload and Today do not refresh it, so yesterday stays 'today' and stays clickable for a new event that the save then refuses as past.

- **CAL-28** A failed side action on an empty calendar replaces the whole grid with the full-page error — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Production with no events. 2. Go offline; open Invitations (or accept from the panel).
  - Expected: The month grid stays; the error shows in the panel or as a toast.
  - Actual: The panel's failure is written to the board's error, and the screen shows the full-page 'Try again' whenever error != null and events are empty - the grid disappears behind the panel.

- **CAL-29** Declined events look identical to accepted ones on the grid — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Decline an invitation. 2. Look at Month and Week view.
  - Expected: Hidden (web fetches hide_rejected=true) or titled 'Declined: ...' and styled.
  - Actual: Desktop fetches hide_rejected=false and draws the chip/block with the plain title; only the popover says Declined.

- **CAL-30** Week/Day drag past midnight parks the event at 23:59 — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Week view: drag a 22:00-23:00 event down three hours.
  - Expected: Moves to 01:00 next day (or the drag is refused).
  - Actual: Start is clamped to 23:59 the same day, so the confirm proposes 23:59-00:59.

- **CAL-31** Month chips, day panel and time-grid blocks use a 24-hour clock; the web (and the desktop popover) use 12-hour — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Look at a 09:00-14:00 event in Month (chip), the day panel and Week view; open its popover.
  - Expected: '9:00 AM - 2:00 PM' everywhere (web ZL-18708).
  - Actual: Chip '9:00', row '09:00 – 14:00'; popover '9:00 AM - 2:00 PM'.

- **CAL-32** Two Create clicks before the button disables create the event twice — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Fill a valid event. 2. Double-click Create very quickly.
  - Expected: One event.
  - Actual: saveForm has no isSaving guard; the button only disables after the next recomposition, so two SaveForm events send two POSTs.

- **CAL-33** Cancelled events vanish instead of showing 'Cancelled: ...' struck through — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Organiser deletes an event you are invited to. 2. Look at the grid.
  - Expected: Shown struck through as 'Cancelled: title' (web), so invitees notice the cancellation.
  - Actual: readEvent drops rows with deleted_at/_isCancelled, so the event just disappears; the popover's 'cancelled' state is reachable only through status='cancelled'.

- **CAL-34** Join Call is still offered after the user declined — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Decline a video meeting. 2. Open it, or look at the day panel.
  - Expected: No Join Call (web ZL-18681).
  - Actual: Join Call shown (desktop deliberately follows the phones).

- **CAL-35** No keyboard support: Esc does not close the form, popover or panels; Enter does not save — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open New Event and press Esc. 2. Open an event popover and press Esc.
  - Expected: Closes (antd Drawer/Modal default).
  - Actual: Nothing happens; there is no key handling in the calendar files or in ZillitDialogShell.

- **CAL-36** The time grid's current-time line never moves — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Leave Week view open for an hour.
  - Expected: Red line tracks the clock.
  - Actual: The time is read once in remember {} and the line stays where it was drawn.
