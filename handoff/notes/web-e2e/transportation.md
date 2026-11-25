# Web E2E — Transportation

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: deep — Passenger side; coordinator side needs the posting right

## Web test cases (164)

### Coordinator — Dashboard

- **header, stat cards, Recent Trips, links and search @smoke** — skipped (transportation-coordinator.spec.js:101)
  - Where: Tools → Transportation → Dashboard · account: iPhone XRR
  - Steps: Open Tools → Transportation (a coordinator lands on the Dashboard) → Check that the Dashboard shows the stat cards “Active Trips”, “Completed Today”, “Pending Requests” and “Available Vehicles”, and the “Recent Trips” card → Type a made-up text such as “zz-no-such-…” in “Search trips…” → Check that “No matching trips” (or “No trips yet”) shows, then clear the search → Click “See all →” and check that the Trips page opens → Click “Dashboard” in the side menu, then “View all trips” → Check that the Trips page opens
  - Expected: The Dashboard shows its four stat cards and Recent Trips; a search with no match says so; “See all →” and “View all trips” both open Trips.

- **the status chips narrow the Recent Trips rows and All restores @smoke** — skipped (transportation-coordinator.spec.js:118)
  - Where: Tools → Transportation → Dashboard, Recent Trips card · account: iPhone XRR
  - Steps: Open Tools → Transportation (a coordinator lands on the Dashboard) → In the Recent Trips card, click the “Active” chip → Check that the card shows trip rows with a “View” button or the message “No matching trips” / “No trips yet” → Repeat for the chips “Pending”, “Completed”, “Cancelled” and finally “All”
  - Expected: After every status chip, including All, the Recent Trips card shows either trip rows or its empty message. The test does not compare which rows each chip keeps.

- **a row's View opens that Trip Detail @smoke** — skipped (transportation-coordinator.spec.js:129)
  - Where: Tools → Transportation → Dashboard, with at least one recent trip · account: iPhone XRR
  - Steps: Open Tools → Transportation (a coordinator lands on the Dashboard) → Click “View” on the first row of Recent Trips → Check that the Trip Detail page opens with “Trip #…” and “ID:”
  - Expected: A Recent Trips row’s View opens that trip’s detail.

- **the "More filters" funnel does nothing (documents current behaviour) @smoke** — skipped (transportation-coordinator.spec.js:138)
  - Where: Tools → Transportation → Dashboard · account: iPhone XRR
  - Steps: Open Tools → Transportation (a coordinator lands on the Dashboard) → Note that no popup, dialog or drawer is open → Click the funnel button (“More filters”) on the Dashboard → Wait a second → Check that still no popup, dialog or drawer has opened
  - Expected: Clicking “More filters” opens nothing. The test documents this current behaviour: the button has no action yet.

### Coordinator — Ride Requests

- **page chrome and the View Active Cars overlay @smoke** — skipped (transportation-coordinator.spec.js:165)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Check that the subtitle shows a count such as “5 requests” and the banner shows “New Request” → Check that the stat cards Pending, Active, Urgent and Completed Today and the “My requests” switch are shown → Click “View Active Cars” → Check that the “Active cars” panel opens with “On duty” and “Available vehicles” → Press Escape (click outside the panel if it stays open) → Check that the panel closes
  - Expected: Ride Requests shows its count, New Request, the four stat cards and the My requests switch; View Active Cars opens a panel of on-duty drivers and available vehicles that closes again.

- **each tab selects and its badge count matches the cards shown @smoke** — skipped (transportation-coordinator.spec.js:184)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click the “Pending” tab and check that it is selected → Read the count on the tab: if it is 0, check that “No pending requests” shows; otherwise check that at least one card has a “Pending” status → Do the same on “Active” (cards marked Approved, Assigned, In transit, En route, At pickup or At dropoff) → Do the same on “Completed” (cards marked Completed or Cancelled) and “Rejected” (cards marked Rejected) → Click “Pending” again
  - Expected: Each tab can be selected; a tab with a count of 0 shows its empty message, and a tab with a count shows at least one card of a matching status. The test does not check that the count equals the number of cards.

- **the stat cards jump to their tab ("Completed Today" counts every completed and cancelled ride) @smoke** — skipped (transportation-coordinator.spec.js:200)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click the “Urgent” stat card and check that the Pending tab is selected → Click the “Active” stat card and check that the Active tab is selected → Click the “Completed Today” stat card and check that the Completed tab is selected → Click the “Pending” stat card and check that the Pending tab is selected
  - Expected: Each stat card switches to its tab: Urgent and Pending to Pending, Active to Active, Completed Today to Completed. The test also notes, without checking it, that “Completed Today” counts every completed and cancelled ride, not only today’s.

- **the "My requests" switch waits for the profile, then scopes the list @smoke** — skipped (transportation-coordinator.spec.js:211)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Wait until the “My requests” switch can be used (it waits for your profile) → Turn the switch on and check that it shows as on → Check that the request count is still shown → Turn the switch off and check that it shows as off
  - Expected: The “My requests” switch becomes usable once your profile has loaded and turns on and off, with the request count still shown. The test does not check which requests the list keeps.

- **empty tabs show their copy @smoke** — skipped (transportation-coordinator.spec.js:223)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Setup: The test makes the server list no ride requests at all (and no rejected ones) in Ride Requests; nothing is saved.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click the “Pending” tab → Check that it says “No pending requests” and “All caught up!” → Click “Active”, “Completed” and “Rejected” in turn → On each, check that it says “No active requests” / “No completed requests” / “No rejected requests” and “Nothing to show in this view.”
  - Expected: With no ride requests, every tab shows its own empty message.

- **the Stops and Passengers chips open their popovers (stub) @smoke** — skipped (transportation-coordinator.spec.js:239)
  - Where: Tools → Transportation → Ride Requests → Active tab · account: iPhone XRR
  - Setup: The test makes the server list one fake assigned ride (pickup “[e2e-th] stub pickup assigned”) with one stop “[e2e-th] stop A” and two passengers “[e2e-th] Pax One” and “[e2e-th] Pax Two” in Ride Requests; nothing is saved.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click the “Active” tab and find the fake ride’s card → Hover over the “1 Stop” chip on the card → Check that a “Route Stops” popup appears → Hover over the “2 Passengers” chip → Check that a “Passengers” popup appears
  - Expected: Hovering the stops and passengers chips on a ride card opens the Route Stops and Passengers popups.

### Coordinator — Request a Ride modal

- **defaults, then the required-field warnings @smoke** — skipped (transportation-coordinator.spec.js:553)
  - Where: Tools → Transportation → Ride Requests → New Request · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu, then “New Request” → Check that the “Request a Ride” dialog shows “Pick Up Now”, “Normal” and “Same as yesterday”, and no guest-passenger section → Click “Request Ride” with nothing filled in → Check the toast “Pickup and dropoff addresses are required” and that the dialog stays open → Type “[e2e-th] pickup” and “[e2e-th] dropoff” in the pickup and dropoff fields → Remove yourself from the passengers and click “Request Ride” → Check the toast “Select at least one passenger” → Check that no ride request was sent, then click “Cancel”
  - Expected: Request a Ride opens with its defaults and will not send without both addresses or without a passenger, showing a message for each.

- **Schedule Time disables past days and past hours today @smoke** — skipped (transportation-coordinator.spec.js:579)
  - Where: Tools → Transportation → Ride Requests → New Request · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu, then “New Request” → Click “Schedule Time”, then the “Select date & time” field → Check that some days (the past ones) are greyed out → Unless it is before 2 AM, check that some hours of today are greyed out too → Press Escape and click “Cancel”
  - Expected: The schedule picker blocks past days and the hours already gone today.

- **Manage stops: add an empty row, remove it, the footer counts, Cancel @smoke** — skipped (transportation-coordinator.spec.js:648)
  - Where: Tools → Transportation → Ride Requests → New Request · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu, then “New Request” → Click “Add stops” (or “Manage stops”) → Check that the “Manage stops” dialog opens with “Search to add a stop…” → Click “Add empty” → Check that it shows “Route stops (1)” and “1 stop ready” → Click “Remove” on the stop and check that it shows “Route stops (0)” → Click “Cancel” in Manage stops, then “Cancel” in Request a Ride
  - Expected: In Manage stops, an empty stop row can be added and removed, and the counts follow.

- **"Same as yesterday" fills the form or explains there is nothing to copy @smoke** — skipped (transportation-coordinator.spec.js:664)
  - Where: Tools → Transportation → Ride Requests → New Request · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu, then “New Request” → Click “↺ Same as yesterday” → Check that a toast says the form was filled from yesterday or from your most recent past ride, or that there is nothing to copy → Click “Cancel”
  - Expected: “Same as yesterday” either fills the form from a past ride or explains that there is nothing to copy.

### Coordinator — Trips and Trip Detail

- **toolbar, Create Trip validation and the passenger cards @smoke** — skipped (transportation-coordinator.spec.js:757)
  - Where: Tools → Transportation → Trips · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Trips” in the side menu → Check that “Filter by date”, “Search passenger or trip id” and the filters All, Pending, Assigned, In Transit, Completed, Cancelled, Rejected are shown → Click “New Trip” and check that “Create New Trip” opens → Click “Create Trip” with nothing filled in → Check that it says “Please fill: Passenger 1: name…” and that there is no “Remove passenger” button → Click “Add another passenger” → Check that “Stop 2” appears and both passengers have a “Remove passenger” button → Remove the second passenger and check that “Stop 2” is gone → Click “Cancel”
  - Expected: Trips shows its date filter, search and seven status filters; Create Trip lists what is missing, and passengers can be added and removed (the last one cannot be removed).

- **date filter and search narrow the list; clearing restores @smoke** — skipped (transportation-coordinator.spec.js:804)
  - Where: Tools → Transportation → Trips, with at least one trip · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Trips” in the side menu → Type “2001-01-01” in “Filter by date” and press Enter → Check that “No trips match these filters.” shows and the subtitle adds the date (“· Jan 1, 2001”) → Clear the date and check that the trips come back → Type the first trip’s six-character id (“#…”) in “Search passenger or trip id” and check that a trip is still listed → Type a made-up text and check that “No trips match these filters.” shows → Clear the search
  - Expected: A date with no trips and a search with no match both empty the list with “No trips match these filters.”; clearing the date restores it, and searching by a trip id keeps that trip listed.

- **a row click, its View button and Enter all open Trip Detail @smoke** — skipped (transportation-coordinator.spec.js:828)
  - Where: Tools → Transportation → Trips, with at least one trip · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Trips” in the side menu → Click “View” on the first trip and check that “Trip #…” shows → Click “Back to trips” → Click anywhere on the first trip row and check that “Trip #…” shows → Click “Back to trips” → Move the keyboard focus to the first trip row and press Enter → Check that “Trip #…” shows
  - Expected: A trip’s detail opens from its View button, from a click on the row, and from Enter on the focused row.

- **past dates and hours are disabled; the Driver select lists "Assign later" first @smoke** — skipped (transportation-coordinator.spec.js:845)
  - Where: Tools → Transportation → Trips → New Trip · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Trips” in the side menu, then “New Trip” → Click “Date” and check that past days are greyed out; press Escape → Click “Pickup time” and, unless it is before 2 AM, check that past hours are greyed out; press Escape → Open the “Driver” list → Check that its first option is “Assign later” → Press Escape and click “Cancel”
  - Expected: Create New Trip blocks past dates and past hours, and the Driver list starts with “Assign later”.

- **classic: a bogus trip id shows an error or "Trip not found" @smoke** — skipped (transportation-coordinator.spec.js:1008)
  - Where: Tools → Transportation → Trips, classic layout · account: iPhone XRR
  - Steps: Open Tools → Transportation (a coordinator lands on the Dashboard) → Type the address of a trip detail page with a made-up trip id (all zeros) into the browser → Check that the page says “Trip not found” or shows an error banner
  - Expected: Opening a trip that does not exist shows “Trip not found” or an error, not a broken screen.

### Coordinator — Fleet

- **list, search, Filter, and the New Vehicle validation @smoke** — skipped (transportation-coordinator.spec.js:1046)
  - Where: Tools → Transportation → Fleet · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Fleet” in the side menu → Check that the search box “Search by name, plate, or type” and the “Filter” button are shown → Click “New Vehicle” and, with nothing filled in, click “Create Vehicle” → Check that “Vehicle name is required” and “License plate is required” show → Untick “Owned by production” and click “Create Vehicle” again → Check that “Owner name is required” and “Owner contact is required” show → Click “Cancel”
  - Expected: Fleet shows its search and Filter; New Vehicle requires a name and a plate, and also an owner name and contact once the vehicle is not owned by production.

- **search is server-side and the filter popover builds the query @smoke** — skipped (transportation-coordinator.spec.js:1063)
  - Where: Tools → Transportation → Fleet · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Fleet” in the side menu → Type “van” in “Search by name, plate, or type” → Check that the app asks the server for vehicles matching “van”, then clear the search → Click “Filter” and, in “Filter vehicles”, pick “Available”, “Car” and “Private” → Click “Apply” → Check that the app asks the server for available, private cars, and that the Filter button shows a badge “3” → Click “Clear filters” and check that the button disappears
  - Expected: The Fleet search and the Filter popup are applied by the server, the Filter badge counts the active filters, and Clear filters removes them.

- **capacity and year are clamped to their bounds @smoke** — skipped (transportation-coordinator.spec.js:1083)
  - Where: Tools → Transportation → Fleet → New Vehicle · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Fleet” in the side menu, then “New Vehicle” → Type 150 in “Seating Capacity”, leave the field, and check that it changes to 99 → Type 0 in “Seating Capacity”, leave the field, and check that it changes to 1 → Type 1900 in “Year”, leave the field, and check that it changes to 1980 → Click “Cancel”
  - Expected: Seating Capacity is held between 1 and 99, and Year cannot go below 1980.

### Coordinator — Drivers

- **stat cards, search and the New Driver validation @smoke** — skipped (transportation-coordinator.spec.js:1173)
  - Where: Tools → Transportation → Drivers · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Drivers” in the side menu → Check that the stat buttons “Total: …”, “Available: …”, “Unavailable: …” and “Incomplete: …” (or “All done”) are shown → Type a made-up text in “Search by name, phone, or license”, wait a moment and clear it → Click “New Driver” and, with nothing filled in, click “Create” → Check that “Select a user”, “Full name is required”, “Phone is required” and “License number is required” show → Type only spaces in the License field and click “Create” → Check that “License number is required” still shows, then click “Cancel”
  - Expected: Drivers shows its stat buttons and search; New Driver requires a user, a full name, a phone and a licence number, and a licence of only spaces does not count. The test does not check the search results.

- **a user without a driver designation locks "Temporary driver" on @smoke** — skipped (transportation-coordinator.spec.js:1197)
  - Where: Tools → Transportation → Drivers → New Driver · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Drivers” in the side menu, then “New Driver” → Open “Search and select a user” and pick the first person listed → Check that “Temporary driver” is ticked and cannot be unticked → Check the note that the user is not from a driver designation → Click “Cancel”
  - Expected: Picking a user who has no driver designation makes them a temporary driver: the box is ticked and locked, with a note explaining why.

### Coordinator — Permanent Allocations

- **list, search, drawer validation and the dirty-close prompt @smoke** — skipped (transportation-coordinator.spec.js:1269)
  - Where: Tools → Transportation → Allocations (titled “Permanent Allocations”) · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Allocations” in the side menu; check the search box and the filters All, Active, Drafts and Paused → Type a made-up text in “Search title, passenger, driver, or address” and check “No permanent allocations match these filters.”; clear it → Click “New Permanent Allocation”, then “Create Allocation” with nothing filled in → Check “Please enter a title”, “Pickup location is required” and “Dropoff location is required” → Click Mon, Tue, Wed, Thu and Fri to turn the days off, click “Create Allocation”, and check “Pick at least one day” → Type a title “[e2e-th] dirty …” and click “Cancel” → In “Save as draft?”, click “Keep editing” and check that the drawer stays open → Click “Cancel” again and choose “Discard” → Check that the drawer closes and nothing was saved
  - Expected: Allocations search reports no match; the New Permanent Allocation drawer requires a title, both locations and at least one day; closing a changed form asks “Save as draft?”, where Keep editing stays and Discard closes without saving.

- **date rules, the option tiles and the passenger row validation @smoke** — skipped (transportation-coordinator.spec.js:1301)
  - Where: Tools → Transportation → Allocations (titled “Permanent Allocations”) → New Permanent Allocation · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Allocations” in the side menu, then “New Permanent Allocation” → Click “Start Date” and check that past days are greyed out; press Escape → Click the “Full Day Trip” tile and check that it switches; click it again → Click the “Self Managed” tile and check that it switches; click it again → Click “Add Passenger” and check that “Passenger 1” appears → Click “Create Allocation” and check “Pick a project user” → Remove the passenger, click “Cancel”, and choose “Discard” if asked
  - Expected: Past start dates cannot be picked, the Full Day Trip and Self Managed tiles switch on and off, and a passenger row needs a project user. The test does not try an end date before the start date.

### Coordinator — Shuttles

- **tabs, drawer defaults and validation @smoke** — skipped (transportation-coordinator.spec.js:1389)
  - Where: Tools → Transportation → Shuttles · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Shuttles” in the side menu; check that “New Route” shows on the Routes tab and disappears on the Runs tab → Back on Routes, click “New Route” → Check the defaults: two stop rows, “Crew”, 15 seats, 06:00 to 22:00, every 30 → Click “Create Route” and check “Starting point is required”, “Destination is required” and “Stop name is required” → Type “[e2e-th] from”, “[e2e-th] to”, “Stop one” and “Stop two”, click “Create Route”, and check “Pick an address on the map” → Click Mon to Fri to turn the days off, click “Create Route”, and check “Select at least one day” → Click “Add stop” and check there are three stop rows; remove one (“Remove stop”) and check there are two → Click “Cancel” and check that the Routes list shows routes or “No shuttle routes defined”
  - Expected: New Route only shows on the Routes tab; the route drawer opens with its defaults and asks for a start, a destination, stop names, map-picked stop addresses and at least one day; stops can be added and removed.

- **Generate runs on an unassigned route warns without a POST; the Runs filter narrows @smoke** — skipped (transportation-coordinator.spec.js:1428)
  - Where: Tools → Transportation → Shuttles, with a route that has no driver · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Shuttles” in the side menu → Find a route card marked “Unassigned” or “No driver” → Click “Generate runs” on it → Check the warning toast “Assign a driver and a vehicle…” and that no runs were requested → Click the “Runs” tab → If the “All routes (n)” filter is shown, pick the second route in it, then click “Clear”
  - Expected: Generate runs on a route without a driver and vehicle only warns and creates nothing. The Runs route filter is opened and cleared, but the test does not check what it lists.

### Coordinator — Live Map

- **banner, pills, tabs and the map canvas @smoke** — skipped (transportation-coordinator.spec.js:1454)
  - Where: Tools → Transportation → Live Tracking (titled “Live Map”) · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Live Tracking” in the side menu (the page is titled “Live Map”) → Check the “Refresh” button and the pills “Live”, “Trips”, “Rides” and “Offline” → Click the “Active Rides” tab and check that it is selected → Check that it shows “No active ride requests”, “Tracking ride” or ride cards → Click the “Active Drivers” tab and check that it is selected → Look for the map (if it does not load, this is only recorded)
  - Expected: Live Map shows Refresh, its four pills and the Active Drivers / Active Rides tabs, which switch. Whether the Google map draws is only recorded.

### Coordinator — Maintenance

- **banner and the Schedule validation @smoke** — skipped (transportation-coordinator.spec.js:1509)
  - Where: Tools → Transportation → Maintenance · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Maintenance” in the side menu and check the subtitle count (“N records”) → Click “Schedule” and check that “Schedule maintenance” opens with “Oil change” → Click “Schedule” in the dialog with no vehicle and check “Select a vehicle” → Click “Pick a date” and check that past days are greyed out → Press Escape and click “Cancel”
  - Expected: Schedule maintenance opens with “Oil change”, requires a vehicle, and blocks past dates.

### Coordinator — Action Vehicles

- **tabs, chip row, sub-tabs, view toggle and the status dropdown @smoke** — skipped (transportation-coordinator.spec.js:1609)
  - Where: Tools → Transportation → Action Vehicles · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Action Vehicles” in the side menu; check the tabs “Requested vehicles”, “Identify”, “Sent for approval” and “Finalized” → On “Requested vehicles”, check the chips All, Requested, Sourced, Pending approval and Rejected, and the “Request a vehicle” button → On “Sent for approval”, check the buttons “Ready to send (n)” and “Awaiting approval (n)” → On “Finalized”, if the “List View” toggle shows, click it and check a table sorted by the Shooting date, then click “Card View” → Otherwise check that it says “No vehicles in “Finalized”” or “Nothing matches your filters”
  - Expected: Action Vehicles shows its four tabs, the request filters, the approval sub-tabs and a working Card / List view. The test does not try the “All statuses” dropdown.

- **the shooting-before-rehearsal rule, and past dates being allowed @smoke** — skipped (transportation-coordinator.spec.js:1692)
  - Where: Tools → Transportation → Action Vehicles → Request a vehicle · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Action Vehicles” in the side menu, then “Request a vehicle” → Set “Need on — rehearsal” to 3 days from today and “Need on — shooting” to 2 days from today → Check “The shooting day cannot be before the rehearsal day.” → Click “Send request” and check that the dialog stays open and nothing is sent → Set the shooting day to the rehearsal day and check that the message goes away → Set rehearsal to 5 days ago and shooting to 4 days ago and check that no error shows → Clear the rehearsal day and check that the order message does not come back → Click “Cancel”
  - Expected: A shooting day before the rehearsal day is refused with a message and blocks sending; the same day is fine. The test also documents a known gap: past dates are accepted in both day fields.

- **the Finalized PDF button follows the download right and the tab contents @smoke** — skipped (transportation-coordinator.spec.js:1896)
  - Where: Tools → Transportation → Action Vehicles → Finalized · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Action Vehicles” in the side menu, then “Finalized” → Look for the “PDF” button (if there is none, the account has no download right; this is recorded and the test ends) → If the tab is empty, check that “PDF” is disabled → Otherwise click “PDF” and check that a file named “action-vehicles….pdf” downloads
  - Expected: With the download right, the Finalized tab’s PDF button downloads an action-vehicles PDF, and is disabled when the tab is empty.

### Coordinator — realtime, badges, MDI

- **sider badges carry a numeric title and the label stays readable @smoke** — skipped (transportation-coordinator.spec.js:1943)
  - Where: Tools → Transportation side menu · account: iPhone XRR
  - Steps: Open Tools → Transportation (a coordinator lands on the Dashboard) → Look at the counts next to the side-menu items → For each count, check that its tooltip holds a plain number → Check that each count shows a number up to 99, or “99+”
  - Expected: Side-menu counts carry a numeric tooltip and never show more than “99+”. The test does not check that long labels stay readable.

- **MDI: the New Request modal renders inside the window body and Escape closes it @smoke** — skipped (transportation-coordinator.spec.js:1956)
  - Where: Tools → Transportation → Ride Requests, in the windowed workspace · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click “New Request” → Check that the Request a Ride dialog opens inside the Transportation window, not over the whole screen → Press Escape → Check that the dialog closes
  - Expected: The New Request dialog opens inside the Transportation window, and Escape closes it.

- **the Finalized PDF gate matches the download right (recorded) @smoke** — skipped (transportation-coordinator.spec.js:1967)
  - Where: Tools → Transportation → Action Vehicles → Finalized · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Action Vehicles” in the side menu, then “Finalized” → Note whether a “PDF” button is shown → Compare it with the account’s download right on Transportation (the test only records it)
  - Expected: Only records whether the Finalized tab shows a PDF button. Nothing about it is checked, so the test passes whenever the page opens.

### Transportation Hub — shell and navigation

- **the Tools tile "Transportation" is present @smoke** — passed (transportation.spec.js:48)
  - Where: Tools · account: iPhone XRR
  - Steps: Open Tools in the sandbox project → Look for the “Transportation” tile → Check that the tile is shown
  - Expected: The Tools page shows a “Transportation” tile.

- **the tile opens the hub @smoke** — passed (transportation.spec.js:55)
  - Where: Tools · account: iPhone XRR
  - Steps: Open Tools in the sandbox project → Click the “Transportation” tile → Check that the Transportation Hub opens with its side menu (within a minute) → In the windowed workspace, check that the window title reads “Transportation”
  - Expected: Clicking the tile opens the Transportation Hub; in the windowed workspace its window is titled “Transportation”.

- **the direct route opens the hub and the role resolves @smoke** — passed (transportation.spec.js:66)
  - Where: Tools → Transportation (opened directly by its web address) · account: iPhone XRR
  - Steps: Open the Transportation Hub directly by its web address (if the project list shows, open SG Document Distribution and try again) → Wait until “Detecting role…” disappears → Check that the hub side menu is shown → Check that the side menu lists at least one page
  - Expected: The hub opens straight from its address, works out the account’s role and shows at least one page in its side menu.

- **the expanded sider shows the title, the back link and the profile card @smoke** — passed (transportation.spec.js:72)
  - Where: Tools → Transportation, side menu expanded · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Check that the side menu shows the heading “Transportation Hub” → Check that it shows the link “Back to Film Tools” → Check that the profile card shows your full name → If you are a project admin, check that the profile card shows “(Admin)”
  - Expected: The expanded side menu shows the “Transportation Hub” title, the “Back to Film Tools” link, and a profile card with your name and “(Admin)” when you are a project admin.

- **collapse hides the title, persists across a reload, and expand restores it @smoke** — passed (transportation.spec.js:89)
  - Where: Tools → Transportation, side menu expanded · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Check that the “Transportation Hub” title shows at the top of the side menu → Click the collapse arrow (“Collapse menu”) → Check that the title is hidden, the page icons stay visible and an “Expand menu” arrow appears → Reload the hub → Check that the side menu is still collapsed (title hidden, “Expand menu” shown) → Click “Expand menu” → Check that the title shows again
  - Expected: Collapsing hides the title but keeps the page icons, the collapsed state survives a reload, and expanding brings the title back and is remembered too.

- **"Back to Film Tools" closes the window (MDI) or lands on the Tools grid (classic) @smoke** — passed (transportation.spec.js:108)
  - Where: Tools → Transportation · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Note whether the hub opened in a window (windowed workspace) or full page (classic layout) → Click “Back to Film Tools” in the side menu → Windowed workspace: check that the Transportation window closes → Classic layout: check that the Tools page with its tiles shows
  - Expected: “Back to Film Tools” closes the Transportation window in the windowed workspace, or returns to the Tools page in the classic layout.

- **every sider item routes to its page and becomes selected @smoke** — passed (transportation.spec.js:121)
  - Where: Tools → Transportation · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Note which pages the side menu lists (a coordinator gets the ten operations pages) → Click each side-menu item in turn → After each click, check that the page title matches the item (for a passenger, Ride Requests opens “My Rides”) → After each click, check that the clicked item is highlighted as selected
  - Expected: Every side-menu item opens its own page and becomes the selected item.

- **the banner theme toggle flips the hub theme and back @smoke** — passed (transportation.spec.js:133)
  - Where: Tools → Transportation · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Check that the banner shows the theme toggle (“Toggle theme”) → Note whether the hub is light or dark → Click the theme toggle → Check that the hub switches to the other theme and the app remembers the new choice → Click the theme toggle again → Check that the hub is back in its first theme
  - Expected: The banner toggle switches the hub between light and dark, the choice is saved, and a second click restores the original theme.

- **MDI: the window body overflow is recorded (known layout issue) @smoke** — passed (transportation.spec.js:147)
  - Where: Tools → Transportation, in the windowed workspace · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Look at the body of the Transportation window → Note whether its content is taller than the window, so the window body scrolls (the known layout issue) → Check that the window body has a visible height
  - Expected: The window body has a visible height. Whether its content overflows (a known layout issue) is only recorded in the report, not checked.

- **MDI: the Call a Driver drawer renders inside the window body and Escape closes it @smoke** — passed (transportation.spec.js:164)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”), in the windowed workspace · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Click “Ride Requests” in the side menu and wait for the “My Rides” page → Click “Call a Driver” → Check that the drawer opens inside the Transportation window, not over the whole screen → Press Escape → Check that the drawer closes
  - Expected: The Call a Driver drawer opens inside the Transportation window, and Escape closes it.

### Transportation Hub — role resolution

- **the role is detected from the sider and recorded @smoke** — passed (transportation.spec.js:181)
  - Where: Tools → Transportation · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Read the side-menu items → Work out the role: “Dashboard” means coordinator, “My Map” or “Assigned Rides” means driver, anything else means passenger → Check that the account resolved to one of these three roles
  - Expected: The hub resolves the account as coordinator, driver or passenger; the role and the side-menu items are recorded in the report.

- **a passenger sees exactly My Trips and Ride Requests, landing on My Trips @smoke** — passed (transportation.spec.js:188)
  - Where: Tools → Transportation · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Check that the side menu lists exactly “My Trips” and “Ride Requests”, in that order → Check that the hub lands on the “My Trips” page → Check that “My Trips” is the selected item
  - Expected: A passenger sees only My Trips and Ride Requests and starts on My Trips.

- **a coordinator sees the ten operations pages in order @smoke** — skipped (transportation.spec.js:198)
  - Where: Tools → Transportation · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Check that the side menu lists exactly Dashboard, Trips, Ride Requests, Fleet, Action Vehicles, Drivers, Allocations, Shuttles, Live Tracking, Maintenance, in that order → Check that the hub lands on the “Dashboard” page
  - Expected: A coordinator sees the ten operations pages in the expected order and starts on the Dashboard.

- **the admin flag alone makes a passenger, labelled "(Admin)" @smoke** — skipped (transportation.spec.js:205)
  - Where: Tools → Transportation · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Confirm from your profile that you are a project admin → Check that the side menu’s profile card shows “(Admin)” → Check that the side menu still lists only “My Trips” and “Ride Requests”
  - Expected: Being a project admin does not make a coordinator: the admin sees “(Admin)” in the profile card but only the passenger pages.

- **classic: passenger deep links to coordinator pages redirect to My Trips @smoke** — skipped (transportation.spec.js:220)
  - Where: Tools → Transportation, classic layout · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Type the web address of the hub’s Trips page into the browser → Check that the hub shows the “My Trips” page instead → Repeat with the address of the Fleet page → Repeat with the address of the Action Vehicles page
  - Expected: A passenger who types the address of a coordinator page (Trips, Fleet, Action Vehicles) is sent to My Trips.

### Transportation Hub — passenger: My Trips

- **My Trips: banner, count subtitle, cards or the empty copy; GET /trip without params @smoke** — passed (transportation.spec.js:264)
  - Where: Tools → Transportation → My Trips · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Check that the “My Trips” page shows → Check that the subtitle shows a count such as “3 trips” → Check that the page shows ride cards (“Ride #…”) or the empty message “No past rides yet” / “No rides match your filters” → Check that the app asked for the trip list without any filter
  - Expected: My Trips shows its title, a trip count and either ride cards or the empty message; the trip list is loaded without filters.

- **TH-P02..P05 with trips: status segments, advanced filters, search and View Details @smoke** — skipped (transportation.spec.js:282)
  - Where: Tools → Transportation → My Trips, with at least one trip · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Type a made-up text such as “zz-no-such-…” in “Search by ride id…” and check that “No rides match your filters.” shows → Clear the search and check that the ride cards come back → Click “Advanced” and check that “Date range” and “Sort by” appear → Click the “Cancelled” status filter and check that a line “Showing x of y rides” appears → Click “Clear” and check that the ride cards come back → Click “View Details” on the first card → Check that a “Ride #…” dialog opens showing “Ride ID:” → Click “Close” and check that the dialog closes
  - Expected: Search, the Advanced filters, the status filter and Clear all work on My Trips, and View Details opens and closes the ride’s detail dialog.

### Transportation Hub — passenger: Ride Requests

- **My Rides: banner, CTA, cards or empty copy; GET /ride-request?me=true&limit=50 @smoke** — passed (transportation.spec.js:365)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”) · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Click “Ride Requests” and wait for the “My Rides” page → Check that the “Call a Driver” button shows → Check that the page shows ride cards (“Ride #…”) or the message “No rides yet…” → Check that the app asked only for your own rides, 50 at a time
  - Expected: My Rides shows its title, the Call a Driver button and either ride cards or the empty message, and loads only your own rides, 50 at a time.

- **the Call a Driver drawer opens with its defaults @smoke** — passed (transportation.spec.js:383)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”) · account: iPhone XRR
  - Setup: The browser shares a location in New Delhi with the site.
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Click “Ride Requests”, then “Call a Driver” → Check that the drawer shows the priorities Urgent, High, Normal and Low, and the options “Pick Up Now” and “Schedule Time” → Check that you are already listed as a passenger, marked “(you)” → Check that “Add Other Passenger” and the notes field “Any special instructions…” are shown → Check that the pickup field “Where are you?” fills in by itself from your location → Click “Cancel” and check that the drawer closes
  - Expected: The Call a Driver drawer opens with its priority and timing options, you as the passenger, the notes field and a pickup address filled from your location.

- **validation: both addresses and at least one passenger are required @smoke** — skipped (transportation.spec.js:401)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”) → Call a Driver · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Click “Ride Requests”, then “Call a Driver” → Clear the pickup field “Where are you?” and click “Call a Driver” in the drawer → Check that “Please select both a pickup and a dropoff location.” shows → Type “[e2e-th] pickup” as pickup and “[e2e-th] dropoff” in “Where are you going?” → Remove yourself (“(you)”) from the passengers and click “Call a Driver” → Check that “Please add at least one passenger.” shows → Check that no ride request was sent → Click “Cancel”
  - Expected: The drawer will not send a ride request without both addresses or without at least one passenger, and shows a message for each.

- **a guest passenger can be added and removed @smoke** — passed (transportation.spec.js:429)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”) → Call a Driver · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Click “Ride Requests”, then “Call a Driver” → Click “Add Other Passenger” and check that “Add” is disabled → Type “Guest <timestamp>” in “Name (required)” and “9999999999” in “Contact number (optional)” → Check that “Add” is enabled, then click it → Check that the guest appears in the passenger list → Click the remove button on the guest (“Remove guest passenger”) and check that the guest is gone → Click “Cancel”
  - Expected: A guest passenger can only be added once a name is typed, appears in the list when added, and can be removed again.

- **Schedule Time shows a date picker with past days disabled (no submit guard) @smoke** — passed (transportation.spec.js:446)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”) → Call a Driver · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Click “Ride Requests”, then “Call a Driver” → Click “Schedule Time” → Check that a “Select date & time” field appears and click it → Check that the calendar opens with some days (the past ones) greyed out → Press Escape, and click “Cancel” if the drawer is still open → Check that the drawer is closed
  - Expected: Schedule Time shows a date-and-time picker in which past days cannot be picked. The test also notes, without checking it, that the drawer has no guard against a past time when you submit.

### Transportation Hub — passenger: geolocation denied

- **without the geolocation permission the drawer explains how to enable it @smoke** — passed (transportation.spec.js:578)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”) · account: iPhone XRR
  - Setup: The browser is not allowed to share your location with the site.
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Click “Ride Requests” and wait for the “My Rides” page → Click “Call a Driver” → Check that an error dialog explains that location access is off and how to turn it on (the settings gear) → Click “OK” and check that the dialog closes
  - Expected: Without location access, Call a Driver shows a dialog that explains how to enable it.

### Transportation Hub — shell extras and legacy routes

- **MDI: re-clicking the Transportation tile re-routes the open window to the role index @smoke** — skipped (transportation.spec.js:597)
  - Where: Tools → Transportation, in the windowed workspace · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Click “Ride Requests” (a coordinator clicks “Trips”) and wait for that page → Go back to Tools → Click the “Transportation” tile again → Check that the open window goes back to “My Trips” (“Dashboard” for a coordinator)
  - Expected: Clicking the tile while the hub window is open sends the window back to the role’s first page again.

- **classic: an unknown sub-path redirects to the role's home @smoke** — skipped (transportation.spec.js:611)
  - Where: Tools → Transportation, classic layout · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Type the hub’s address followed by a made-up page name (“nope-…”) into the browser → Check that the hub opens “My Trips” (“Dashboard” for a coordinator) → Check that the made-up page name is gone from the address bar
  - Expected: An unknown hub page address redirects to the role’s first page instead.

- **classic: the hub route hides the app side menu (full bleed) @smoke** — skipped (transportation.spec.js:620)
  - Where: Tools → Transportation, classic layout · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Check that the app’s own side menu (with “Home”) is hidden → Check that the hub’s side menu is shown
  - Expected: In the classic layout the hub fills the page: the app’s side menu is hidden and only the hub’s own side menu shows.

- **without an active project the hub sends the user to the project list @smoke** — passed (transportation.spec.js:627)
  - Where: Tools → Transportation · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Clear the remembered current project from the browser’s storage → Open the hub’s web address again → Check that the Projects list shows instead of the hub
  - Expected: With no active project, opening the hub sends you to the Projects list.

- **classic: the Track Driver route without a ride shows the empty copy @smoke** — skipped (transportation.spec.js:641)
  - Where: Tools → Transportation → Track Driver, classic layout · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Type the address of the hub’s Track Driver page into the browser → Check that the “Track Driver” page shows → Check that it says “No driver is currently allocated or assigned…” → Click “Back”
  - Expected: Track Driver without an assigned ride shows the message that no driver is allocated or assigned.

- **classic: the legacy transportation tiles and the posting-gated vehicle list @smoke** — skipped (transportation.spec.js:651)
  - Where: The old Transportation pages (reached by their web addresses), classic layout · account: iPhone XRR
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Type the address of the old Transportation page into the browser → If the Tools page shows instead, stop: the account has no view right on the old tool (recorded, not a failure) → Check that the tiles “Raise Pickup Request” and “Track Pickup Status” show → Type the address of the old Vehicle List page → Coordinator: check that the vehicle list opens → Anyone else: check that you are sent back to the old Transportation tiles or to Tools
  - Expected: The old Transportation page shows its tiles, and its Vehicle List opens only for a coordinator (anyone else is sent back).

### Transportation Hub — role resolution

- **a failing rights call resolves the account as a passenger without crashing** — skipped (transportation.spec.js:232)
  - Where: Tools → Transportation (opened directly by its web address) · account: iPhone XRR
  - Setup: The test makes the server fail the project’s tool-rights list with a plain server error.
  - Steps: Open the Transportation Hub directly by its web address → Wait until “Detecting role…” disappears → Check that the side menu shows the passenger pages (no Dashboard, no driver pages) → Check that the app does not show its “Something went wrong.” crash screen
  - Expected: When the rights list fails to load, the hub falls back to the passenger role and keeps working.

- **granting / revoking the transport posting right flips the open hub live** — skipped (transportation.spec.js:255)

- **adding B as a temp driver flips B to the driver nav** — skipped (transportation.spec.js:256)

- **TH-R01..R04 driver surface: duty pings, assigned-ride chain, My Trips, trimmed detail** — skipped (transportation.spec.js:257)

- **TH-S01..S04 realtime between passenger and coordinator processes** — skipped (transportation.spec.js:258)

- **TH-T01..T03 sider badges, read-on-open and silent edits** — skipped (transportation.spec.js:259)

### Transportation Hub — passenger: My Trips

- **a refused or broken /trip list shows an error surface and nothing crashes** — passed (transportation.spec.js:312)
  - Where: Tools → Transportation → My Trips · account: iPhone XRR
  - Setup: The test makes the server refuse the trip list with the message code “x_y”.
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Check that the “My Trips” page shows → Check that an error shows: the message “X Y”, an error banner or a toast → Check that the app does not show “Something went wrong.” → Now make the trip list fail with a plain server error (no message) → Click “Ride Requests”, then “My Trips” again → Wait three seconds and check that “Something went wrong.” still does not show
  - Expected: A refused trip list shows an error message, and a broken one is handled quietly; neither crashes the screen.

### Transportation Hub — passenger: Ride Requests

- **create a ride request, edit it while pending, then cancel it** — skipped (transportation.spec.js:465)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”) · account: iPhone XRR
  - Setup: Creates a ride request through Call a Driver, with pickup “[e2e-th] <timestamp> pickup”, dropoff “… dropoff” and notes “… notes”.
  - Steps: Open Tools → Transportation, click “Ride Requests”, then “Call a Driver” → Type the e2e pickup, dropoff and notes, then click “Call a Driver” in the drawer → Check that the request goes out as your own ride and the drawer closes → Check that a new “Ride #…” card with the e2e text shows “Waiting for coordinator approval” → Click “Edit” on the card, check that “Edit Ride Request” opens with the notes filled in → Change the notes to “… notes edited” and click “Save Changes” → Check that the toast “Ride updated” shows and the card shows the new notes → Click “Cancel” on the card, then “Keep ride”, and check that the ride is still waiting → Click “Cancel” again, type “e2e cleanup” in “Reason (optional)” and click “Cancel ride” → Check that the toast “Ride cancelled” shows and the card no longer says it is waiting
  - Expected: A passenger can request a ride, edit it while it is pending, and cancel it; “Keep ride” leaves it untouched.

- **a refused cancel shows the curated fallback copy (stubbed list and DELETE)** — flaky (transportation.spec.js:521)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”) · account: iPhone XRR
  - Setup: The test makes the server list one fake pending ride (pickup “[e2e-th] stub pickup”) and refuse to cancel it with the code “ride_request_cannot_be_cancelled”.
  - Steps: Open Tools → Transportation and wait until the side menu shows its pages (“Detecting role…” is gone) → Click “Ride Requests” and find the ride card with “[e2e-th] stub pickup” → Click “Cancel” on that card → Click “Cancel ride” in the “Cancel ride request?” dialog → Check that the message “This ride request can no longer be cancelled.” shows
  - Expected: When the server refuses a cancel, the app shows the friendly message “This ride request can no longer be cancelled.”
  - Web error: Error: expect(locator).toBeVisible() failed

- **Track driver on an assigned ride (needs a coordinator to assign)** — skipped (transportation.spec.js:571)

- **classic: the Track Driver route without a ride shows the empty copy** — skipped (transportation.spec.js:572)

### Coordinator — Dashboard

- **a failing insights call shows an error alert while the other cards still render** — skipped (transportation-coordinator.spec.js:147)
  - Where: Tools → Transportation → Dashboard · account: iPhone XRR
  - Setup: The test makes the server fail the Dashboard’s insights (the stat figures) with a server error.
  - Steps: Open Tools → Transportation (a coordinator lands on the Dashboard) → Check that an alert banner shows on the Dashboard → Check that the “Recent Trips” card and the “Pending Requests” card are still shown → Check that the app does not show “Something went wrong.”
  - Expected: When the insights fail to load, the Dashboard shows an alert but keeps rendering its other cards.

### Coordinator — Ride Requests

- **load contract: the newest list plus the rejected list; a rejected row from the second shows under Rejected (stub)** — skipped (transportation-coordinator.spec.js:257)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Setup: The test makes the server list one fake pending ride, and one fake rejected ride (pickup “[e2e-th] stub pickup rejected”) only in the separate list of rejected rides in Ride Requests; nothing is saved.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Check that the app asked for two lists: the ride requests without any filter, and the rejected requests (up to 200) → Click the “Rejected” tab → Check that the fake rejected ride’s card is listed
  - Expected: Ride Requests loads the main list and a separate list of rejected requests, and a ride that only appears in the rejected list shows under Rejected.

- **reject and approve requests the test created** — skipped (transportation-coordinator.spec.js:274)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Setup: Creates two ride requests through New Request: “[e2e-th] reject <timestamp> pickup / dropoff” and “[e2e-th] approve <timestamp> pickup / dropoff”.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click “New Request”, type the “[e2e-th] reject …” pickup and dropoff, and click “Request Ride” → On the Pending tab, click “Reject” on that card → In “Reject Ride Request”, type “e2e reject reason” and click “Reject Request” → Click the “Rejected” tab and check that the request is listed there → Create the “[e2e-th] approve …” request the same way → Click “Approve” on its card → Click the “Active” tab and check that the card shows “Approved” and an “Assign Driver” button → Click “Cancel Trip” on that card and confirm with “Cancel Trip”
  - Expected: A coordinator can reject a request with a reason (it moves to Rejected) and approve another (it moves to Active as Approved, ready for Assign Driver).

- **rejecting with an empty reason sends "Request declined" (stub)** — skipped (transportation-coordinator.spec.js:311)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Setup: The test makes the server list one fake pending ride (pickup “[e2e-th] stub pickup pending”) and answer its reject with success in Ride Requests; nothing is saved.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click “Reject” on the fake ride’s card → In “Reject Ride Request”, leave the reason empty → Click “Reject Request” → Check that the reject went out with the reason “Request declined”
  - Expected: Rejecting without typing a reason sends the default reason “Request declined”.

- **Approve & Assign: drawer, roster states and the assign body (stub)** — skipped (transportation-coordinator.spec.js:335)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Setup: The test makes the server list one fake pending ride, a driver roster with one on-duty driver “[e2e-th] Stub Driver”, and no available vehicles; the assignment is answered with success.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click “Approve & Assign” on the fake ride’s card → Check that the Assign Driver drawer opens with “No vehicles are available right now.” → Check that the drawer’s confirm button is disabled → Type “zz-none” in “Search drivers…” and check that “No drivers match” shows, then clear it → Click “[e2e-th] Stub Driver” and check that the confirm button is enabled → Click the confirm button → Check that the toast “Driver assigned” shows and the assignment went out for that driver
  - Expected: The Approve & Assign drawer needs a driver picked before it can confirm; search filters the roster; confirming assigns the picked driver and shows “Driver assigned”.

- **Cancel Trip: Keep Trip leaves it, Cancel Trip sends DELETE with the default reason (stub)** — skipped (transportation-coordinator.spec.js:382)
  - Where: Tools → Transportation → Ride Requests → Active tab · account: iPhone XRR
  - Setup: The test makes the server list one fake approved ride (pickup “[e2e-th] stub pickup approved”) and answer its cancel with success in Ride Requests; nothing is saved.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click the “Active” tab and click “Cancel Trip” on the fake ride’s card → Check that the “Cancel Trip” dialog says “This can’t be undone.” → Click “Keep Trip” and check that the dialog closes and nothing was sent → Click “Cancel Trip” on the card again and confirm with “Cancel Trip” → Check that a cancel went out for the ride with the reason “Trip cancelled”
  - Expected: “Keep Trip” leaves the ride alone; confirming “Cancel Trip” cancels it with the reason “Trip cancelled”.

- **Edit Trip on a pending row; no Edit on in-transit or terminal rows; the not-editable code shows a fallback (stub)** — skipped (transportation-coordinator.spec.js:413)
  - Where: Tools → Transportation → Ride Requests · account: iPhone XRR
  - Setup: The test makes the server list three fake rides — pending, in transit and completed — and answer the first edit with success in Ride Requests; nothing is saved.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click “Edit” on the pending fake ride and check that “Edit Trip” opens with the pickup filled in → Click “High”, type “e2e edited notes” in “Special instructions…” and click “Save Changes” → Check that the toast “Ride request updated” (or “Trip updated”) shows → Click the “Active” tab and check that the in-transit ride has no “Edit” button → Click the “Completed” tab and check that the completed ride has no “Edit” button → Now make the server refuse edits as “not editable” → Back on “Pending”, click “Edit” and then “Save Changes” → Check that an error toast or alert shows, then click “Cancel”
  - Expected: A pending request can be edited and saved; in-transit and completed requests offer no Edit; when the server refuses the edit as not editable, the app shows a message.

- **Add Passenger, Add Stop and complete-stop on an active ride (stub)** — skipped (transportation-coordinator.spec.js:457)
  - Where: Tools → Transportation → Ride Requests → Active tab · account: iPhone XRR
  - Setup: The test makes the server list one fake assigned ride with one pending stop “[e2e-th] stop A”, and answer every change to it with success in Ride Requests; nothing is saved.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click “Active”, then “Add Passenger” on the fake ride’s card → Confirm without picking anyone and check the toast “Please select a passenger from the project” → Open “Choose from project users”, pick the first user, confirm and check the toast “Passenger added” → Click “Add Stop” and confirm without an address; check the toast “Stop address is required” → Type “[e2e-th] new stop” in “Search stop address…” → In “Pick a project user or type a guest name”, type “Guest Stopper”, press Enter, then Escape → Confirm and check the toast “Stop added” (the stop goes out as a waypoint with coordinates) → Hover the “1 Stop” chip and, in the “Route Stops” popup, click the stop’s button to complete it → Check that the stop was marked as completed
  - Expected: Add Passenger and Add Stop refuse an empty form with a message and succeed once filled; a stop can be completed from the Route Stops popup.

- **Start Ride → Complete on an assigned request (stub)** — skipped (transportation-coordinator.spec.js:515)
  - Where: Tools → Transportation → Ride Requests → Active tab · account: iPhone XRR
  - Setup: The test makes the server list one fake assigned ride (pickup “[e2e-th] stub pickup assigned”) and answer its status change with success in Ride Requests; nothing is saved.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click the “Active” tab and find the fake ride’s card → Click “Start Ride” → Check that exactly one status update was sent for the ride
  - Expected: Start Ride sends one status update for the ride. Despite the title, the test stops there and does not press Complete.

- **a request created by account B appears live and bumps the sider badge** — skipped (transportation-coordinator.spec.js:539)

### Coordinator — Request a Ride modal

- **a scheduled time that slips into the past is refused on submit** — skipped (transportation-coordinator.spec.js:591)
  - Where: Tools → Transportation → Ride Requests → New Request · account: iPhone XRR
  - Setup: The test takes control of the browser clock so it can move time forward.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu, then “New Request” → Type “[e2e-th] pickup” and “[e2e-th] dropoff” in the pickup and dropoff fields → Click “Schedule Time” and type a time one minute from now in “Select date & time” (for example “16 Nov 2026, 14:31”), then press Enter → Let three minutes pass → Click “Request Ride” → Check the toast “Scheduled pickup time cannot be in the past” and that no request was sent → Click “Cancel”
  - Expected: A scheduled time that has slipped into the past by the time you submit is refused with “Scheduled pickup time cannot be in the past”.

- **create with typed addresses (coordinator call type), then an Urgent one** — skipped (transportation-coordinator.spec.js:617)
  - Where: Tools → Transportation → Ride Requests → New Request · account: iPhone XRR
  - Setup: Creates two ride requests through New Request: “[e2e-th] coordinator-ride <timestamp> pickup / dropoff” and an Urgent “… urgent pickup / urgent dropoff”.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu, then “New Request” → Type the e2e pickup and dropoff and click “Request Ride” → Check that the request goes out as made by a coordinator, with the pickup’s coordinates → Check that the dialog closes and the request is listed under Pending → Note the number on the “Urgent” stat card → Click “New Request”, click “Urgent”, type the e2e urgent pickup and dropoff and click “Request Ride” → Check that the new card shows an “Urgent” label → Check that the Urgent stat number went up
  - Expected: A coordinator can create a ride request from typed addresses (it appears under Pending), and an Urgent request is labelled Urgent and raises the Urgent count.

- **a backend error code is humanised in the alert and the toast (stub)** — skipped (transportation-coordinator.spec.js:672)
  - Where: Tools → Transportation → Ride Requests → New Request · account: iPhone XRR
  - Setup: The test makes the server refuse new ride requests with the error code “some_code”.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu, then “New Request” → Type “[e2e-th] pickup” and “[e2e-th] dropoff” → Click “Request Ride” → Check that the error shows in readable form as “Some Code” → Click “Cancel”
  - Expected: A server error code is shown to the user in readable words (“Some Code”).

- **create with map-picked stops issues follow-up add-stop calls (Google Maps)** — skipped (transportation-coordinator.spec.js:690)

### Coordinator — Assign drawer

- **empty roster, nobody on duty, search, the Add link and the no-vehicles alert (stub)** — skipped (transportation-coordinator.spec.js:695)
  - Where: Tools → Transportation → Ride Requests → Active tab → Assign Driver · account: iPhone XRR
  - Setup: The test makes the server list one fake approved ride, an empty driver roster, nobody on duty and no available vehicles.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click the “Active” tab, then “Assign Driver” on the fake ride’s card → Check that the drawer says “No vehicles are available right now.” → Check that it says “No drivers on this project yet” or “Nobody is on duty…” → Check that the “Assign” button is disabled → Click the “Add” link → Check that the drawer closes and the Drivers page opens
  - Expected: With no drivers and no vehicles, the Assign drawer explains both, cannot assign, and its Add link leads to Drivers. Despite the title, the test does not try the driver search.

- **flipping a driver on duty from the drawer (changes a real driver)** — skipped (transportation-coordinator.spec.js:722)

- **Reassign from Trip Detail → PUT /trip/:id/transfer (notifies the drivers)** — skipped (transportation-coordinator.spec.js:723)

### Coordinator — Trips and Trip Detail

- **the status segments group the backend statuses (stub)** — skipped (transportation-coordinator.spec.js:779)
  - Where: Tools → Transportation → Trips · account: iPhone XRR
  - Setup: The test makes the server list seven fake trips: en route, at pickup, in transit, at dropoff, draft, approved and completed.
  - Steps: Open Tools → Transportation and click “Trips” in the side menu → Click “In Transit” and check that 4 trips are listed → Click “Pending” and check that 2 trips are listed → Click “Completed” and check that 1 trip is listed → Click “All” and check that all 7 trips are listed
  - Expected: The status filters group the trip states: In Transit covers en route, at pickup, in transit and at dropoff; Pending covers draft and approved.

- **a time set for a future date survives switching the date back to today (documents the gap)** — skipped (transportation-coordinator.spec.js:867)
  - Where: Tools → Transportation → Trips → New Trip · account: iPhone XRR
  - Setup: The test catches the new trip before it reaches the server and answers it with success; nothing is saved.
  - Steps: Open Tools → Transportation and click “Trips” in the side menu, then “New Trip” → Set “Date” to tomorrow → Set “Pickup time” to 00:05 → Set “Date” back to today → Type “[e2e-th] bypass” as the passenger Name, and “[e2e-th] pickup” / “[e2e-th] dropoff” as the addresses → Click “Create Trip” → Check that the trip is sent
  - Expected: The trip is sent even though its pickup time (00:05 today) is already past. The test documents this gap: Create Trip does not check for a past time when you submit.

- **create a trip with yourself as the passenger, then delete it** — skipped (transportation-coordinator.spec.js:904)
  - Where: Tools → Transportation → Trips · account: iPhone XRR
  - Setup: Creates a trip “[e2e-th] trip <timestamp>” through New Trip.
  - Steps: Open Tools → Transportation and click “Trips” in the side menu, then “New Trip” → Type the e2e trip name in “Trip Title” → Open “Search project members…” and pick the first member; check that Name fills in → Type the e2e pickup and dropoff addresses and click “Create Trip” → Check that the drawer closes and the trip is listed as “pending” (its pickup goes out with coordinates) → Click “Delete” on the trip and confirm “Delete this trip?” with “Delete” → Check the toast “Trip deleted” and that the trip is gone
  - Expected: A coordinator can create a trip for a project member, see it listed as pending, and delete it.

- **a ride-request row in Trips can be cancelled; terminal ones offer no Cancel (stub)** — skipped (transportation-coordinator.spec.js:924)
  - Where: Tools → Transportation → Trips · account: iPhone XRR
  - Setup: The test makes the server list two fake ride-request rows, “[e2e-th] stub ride row” (pending) and “[e2e-th] stub done ride” (completed), and answers the cancel with success.
  - Steps: Open Tools → Transportation and click “Trips” in the side menu → Check that the completed row “[e2e-th] stub done ride” has no “Cancel” button → Click “Cancel” on the pending row “[e2e-th] stub ride row” → Check that the confirmation asks “Cancel this ride request?” and click “Cancel ride” → Check the toast “Ride request cancelled” and that the cancel was sent
  - Expected: A pending ride request listed in Trips can be cancelled after a confirmation; a completed one offers no Cancel.

- **Trip Detail of an own trip: header, no-driver hints, cancel via transition, timeline, back** — skipped (transportation-coordinator.spec.js:953)
  - Where: Tools → Transportation → Trips → a new trip’s detail · account: iPhone XRR
  - Setup: Creates a trip “[e2e-th] detail <timestamp>” through New Trip (first project member as passenger, no driver).
  - Steps: Open Tools → Transportation and click “Trips” in the side menu and create the e2e trip with New Trip → Click “View” on it and check “Trip #…”, “ID:”, “1 Passenger” and “Trip Title” → Check the hint “Assign a driver from the trip header…” and that “Send reminder” is disabled → Check that the timeline shows “No timeline entries yet” or its entries → Click “Cancel” and confirm “Transition to “cancelled”?” with “Yes” → Check the toast “Status updated to cancelled” → Check that the page says the trip is cancelled and no further actions are available, with a “Reason” block → Click “Back to trips” and check that the Trips list shows
  - Expected: Trip Detail of a new trip without a driver shows its header, the no-driver hint, a disabled Send reminder and the timeline; cancelling it moves it to a final cancelled state with a reason.

- **the passenger search appears only with more than four passengers (stub detail)** — skipped (transportation-coordinator.spec.js:985)
  - Where: Tools → Transportation → Trips → trip detail · account: iPhone XRR
  - Setup: The test makes the server list one fake pending trip with five passengers “[e2e-th] Pax 1” to “[e2e-th] Pax 5”, and serves its detail.
  - Steps: Open Tools → Transportation and click “Trips” in the side menu → Click “View” on the fake trip → Check that a passenger search box “Search 5 passengers…” is shown
  - Expected: A trip with five passengers shows a passenger search on its detail. The test does not check that it is hidden with four or fewer.

- **status transitions on a trip with a driver (needs a driver record)** — skipped (transportation-coordinator.spec.js:1015)

- **Send reminder (stub only — notifies passengers and the driver)** — skipped (transportation-coordinator.spec.js:1016)

- **create with a driver → PUT /trip/:id/assign (notifies the driver)** — skipped (transportation-coordinator.spec.js:1017)

### Coordinator — Fleet

- **create, duplicate plate, custom type, edit, status, delete** — skipped (transportation-coordinator.spec.js:1101)
  - Where: Tools → Transportation → Fleet · account: iPhone XRR
  - Setup: Creates a vehicle “[e2e-th] Van <timestamp>” with plate “E2E-<timestamp>” through New Vehicle.
  - Steps: Open Tools → Transportation and click “Fleet” in the side menu, click “New Vehicle”, type the e2e Name and License Plate and click “Create Vehicle” → Check the toast “Vehicle created” and that the new card shows the name → Click “New Vehicle” again with the same plate and click “Create Vehicle”; check that an “already exists” error shows, then Cancel → Click Edit on the card and type “E2E Teal” in “Color” → If the type list offers “Add a type — e.g. Bike, SUV, Tempo”, add “E2E Type” and check it is tagged “Custom” → Click “Save Changes” and check the toast “Vehicle updated” → Open Edit again, remove the custom type “E2E Type” if it is shown, and Cancel → Change the card’s status to “maintenance” and check that it shows “Maintenance”, then set it back to “available” → Click Delete on the card, confirm “Delete vehicle?” with “Delete”, and check the card is gone
  - Expected: A vehicle can be created, a second one with the same plate is refused, the vehicle can be edited (colour, custom type), its status changed and the vehicle deleted.

- **vehicle photos: an image previews, a non-image is refused, the tile hides at three (documents)** — skipped (transportation-coordinator.spec.js:1152)
  - Where: Tools → Transportation → Fleet → New Vehicle · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Fleet” in the side menu, then “New Vehicle” → Add a text file (“not-image.txt”) under Vehicle Photos and check the toast “Only image files are allowed” → Add the image “red.png” and check that a thumbnail appears → Add “blue.png” and “red.png” together and check that there are three thumbnails → Check that the “Upload” tile is gone → Click “Cancel”
  - Expected: Photos must be images and show as thumbnails before saving. The test documents a known gap: the “Upload” tile disappears after three photos although the label says “up to 4 images”.

### Coordinator — Drivers

- **create a driver record for yourself, toggle duty, delete it** — skipped (transportation-coordinator.spec.js:1213)
  - Where: Tools → Transportation → Drivers · account: iPhone XRR
  - Setup: Creates a driver record through New Driver for the first person in the user list, with phone 9999999999 and licence “E2E-<timestamp>”.
  - Steps: Open Tools → Transportation and click “Drivers” in the side menu, then “New Driver” → Open “Search and select a user” and pick the first person listed → Type 9999999999 as phone and “E2E-<timestamp>” as licence number, then click “Create” → Check that the dialog closes and a driver card with that person’s name appears → Flip the card’s duty switch and check that it changes → Flip it back and check that it returns to how it was → Click Delete on the card, confirm “Remove driver?” with “Delete”, and check the card is gone
  - Expected: A driver record can be created for a project member, put on and off duty, and removed.

- **Ignore & create → Incomplete profiles → Complete details; licence photos (needs a throwaway member)** — skipped (transportation-coordinator.spec.js:1246)

### Coordinator — Permanent Allocations

- **save a draft from the dirty prompt, edit it, delete it** — skipped (transportation-coordinator.spec.js:1326)
  - Where: Tools → Transportation → Allocations (titled “Permanent Allocations”) · account: iPhone XRR
  - Setup: Creates a draft allocation “[e2e-th] draft <timestamp>” from the “Save as draft?” prompt.
  - Steps: Open Tools → Transportation and click “Allocations” in the side menu, then “New Permanent Allocation” → Type the e2e title in “Title” and click “Cancel” → In “Save as draft?”, click “Save draft” and check that the drawer closes → Click the “Drafts” filter and check that the card with the title shows “DRAFT” → Click “Edit” on the card and check that the title is filled in → Change the title to “… edited” and click “Save Changes” → Check that the card shows the edited title → Click the red delete button on the card and confirm “Delete this allocation?” with “Delete”
  - Expected: A form closed with “Save draft” becomes a DRAFT allocation that can be edited and deleted.

- **"Create Allocation" with typed addresses POSTs a draft or surfaces a coordinates error** — skipped (transportation-coordinator.spec.js:1355)
  - Where: Tools → Transportation → Allocations (titled “Permanent Allocations”) → New Permanent Allocation · account: iPhone XRR
  - Setup: Tries to create an allocation “[e2e-th] typed <timestamp>” with typed (not map-picked) addresses.
  - Steps: Open Tools → Transportation and click “Allocations” in the side menu, then “New Permanent Allocation” → Type the e2e title, “[e2e-th] pickup” as pickup and “[e2e-th] dropoff” as dropoff → Click “Create Allocation” → Check that either a card with the title appears or a toast shows → If the allocation was sent, check that it went out as a draft → If a card appeared, delete it; otherwise note the toast, click “Cancel” and choose “Discard”
  - Expected: Creating with typed addresses either saves a draft allocation or shows a message (the message is recorded in the report).

- **create with map coordinates, pause / resume, swap driver / vehicle (Google Maps, notifies)** — skipped (transportation-coordinator.spec.js:1384)

### Coordinator — Shuttles

- **create a route (map stops), generate runs, run transitions, enable / disable, edit, delete, end-before-start (Google Maps, notifies)** — skipped (transportation-coordinator.spec.js:1449)

### Coordinator — Live Map

- **the map polls /location/active and Refresh fires at once** — skipped (transportation-coordinator.spec.js:1473)
  - Where: Tools → Transportation → Live Tracking (titled “Live Map”) · account: iPhone XRR
  - Steps: Open Tools → Transportation and click “Live Tracking” in the side menu → Check that the page asks the server for the drivers’ live locations within 20 seconds → Wait and check that it asks again by itself within the next 25 seconds → Click “Refresh” → Check that it asks again within 5 seconds
  - Expected: The Live Map refreshes the live locations by itself every few seconds, and Refresh fetches them at once.

- **a failing /location/active shows an alert while rides still render (stub)** — skipped (transportation-coordinator.spec.js:1489)
  - Where: Tools → Transportation → Live Tracking (titled “Live Map”) · account: iPhone XRR
  - Setup: The test makes the server fail the live-locations list with a server error.
  - Steps: Open Tools → Transportation and click “Live Tracking” in the side menu → Check that an alert banner shows → Check that the “Active Rides” tab is still shown → Check that the app does not show “Something went wrong.”
  - Expected: When live locations fail to load, the Live Map shows an alert and keeps working.

- **selecting a ride shows "Tracking ride" and Clear (needs an active ride)** — skipped (transportation-coordinator.spec.js:1504)

### Coordinator — Maintenance

- **schedule maintenance for a test vehicle, complete it, delete it** — skipped (transportation-coordinator.spec.js:1527)
  - Where: Tools → Transportation → Maintenance · account: iPhone XRR
  - Setup: Creates a vehicle “[e2e-th] Maint Van <timestamp>” with plate “E2E-<timestamp>” in Fleet.
  - Steps: Open Tools → Transportation and click “Fleet” in the side menu, create the e2e vehicle with “New Vehicle” and check the toast “Vehicle created” → Click “Maintenance”, then “Schedule” → In “Search and select a vehicle”, pick the e2e vehicle by its plate → Type “[e2e-th] oil” in “What needs doing” and click “Schedule” → Check that a card for the vehicle appears with “Oil change” → Click “Complete” and check that the button disappears and the card shows completed → Click “Delete”, confirm “Delete this maintenance record?” with “Delete”, and check the card is gone
  - Expected: Maintenance can be scheduled for a vehicle, marked complete and deleted.

### Coordinator — Action Vehicles

- **pagination walks page=1,2,… until an empty page and stops on a repeated page (stub)** — skipped (transportation-coordinator.spec.js:1634)
  - Where: Tools → Transportation → Action Vehicles → Requested vehicles · account: iPhone XRR
  - Setup: The test makes the server answer the Action Vehicles list page by page: two fake requests “[e2e-th] stub AV requested” on page 1, one on page 2, and nothing on page 3.
  - Steps: Open Tools → Transportation and click “Action Vehicles” in the side menu → Click “Requested vehicles” → Check that the app asked for page 1, then page 2, then page 3 → Check that exactly three fake request cards are shown
  - Expected: The Action Vehicles list keeps loading pages until one comes back empty and shows every record from them. The test does not try a page that repeats.

- **blank request → card, edit it, History drawer, detail sections, remove** — skipped (transportation-coordinator.spec.js:1658)
  - Where: Tools → Transportation → Action Vehicles → Requested vehicles · account: iPhone XRR
  - Setup: Creates a blank Action Vehicle request (it shows as “Untitled Request”).
  - Steps: Open Tools → Transportation and click “Action Vehicles” in the side menu, then “Requested vehicles” → Click “Request a vehicle” and click “Send request” without filling anything → Check the toast “Vehicle requested…” and an “Untitled Request” card marked “Requested” → Click “Edit request”, type “[e2e-th] AV edited <timestamp>” as Display name and click “Save changes”; check the toast “Saved” → Click the renamed card and check that its detail shows a “Request” section but no “Source & agreement” → Click “History” and check that “History · …” opens with “Everything that happened to this request…” → Press Escape twice and check that both drawers are closed → Click the trash button on the card, confirm “Remove …?” with “Remove”, and check the toast “… removed”
  - Expected: A blank vehicle request is accepted as “Untitled Request”; it can be renamed, its detail and History open, and it can be removed while still requested.

- **a full request: category (custom), vehicle option editor, scenes, location, dates, reminder, driver, notes** — skipped (transportation-coordinator.spec.js:1724)
  - Where: Tools → Transportation → Action Vehicles → Requested vehicles · account: iPhone XRR
  - Setup: Creates an Action Vehicle request “[e2e-th] AV full <timestamp>” with every field filled, and a custom category “E2E Cat”.
  - Steps: Open Tools → Transportation and click “Action Vehicles” in the side menu, click “Requested vehicles”, then “Request a vehicle”, and type the e2e Display name → In Category pick “Hero” and, if offered, add the custom category “E2E Cat” → Click “Add vehicle option”: check “Save option” is disabled, type “Ford” and “Mustang”, check it is enabled and click it → Check that “Ford … Mustang” is listed as an option → Add scene 12, type “[e2e-th] Stage 4” as Location, and set rehearsal 5 and shooting 6 days ahead → Add “E2E Driver” under Approved drivers and type “[e2e-th] notes” in Notes → Click “Send request” and check the toast “Vehicle requested…” → Check that the card shows “Stage 4” and scene 12; open it and check “Requested vehicle options” lists “Ford” → Click “Edit request”, remove the custom category “E2E Cat”, and Cancel
  - Expected: A full vehicle request is saved with its category, vehicle option, scene, location, dates, driver and notes, and they show on the card and in the detail. The shoot-day reminder named in the title is not set.

- **Source validation and the scalar /source body (stub write)** — skipped (transportation-coordinator.spec.js:1791)
  - Where: Tools → Transportation → Action Vehicles → Identify · account: iPhone XRR
  - Setup: Creates an Action Vehicle request “[e2e-th] AV source <timestamp>”; the test catches the sourcing save and answers it with success, so the record is never sourced.
  - Steps: Open Tools → Transportation and click “Action Vehicles” in the side menu and request a vehicle named with the e2e name → Click “Identify”, then “Source” on the card; check that “Source: …” shows “Requested by AD” → Click “Save & mark sourced” with nothing chosen and check that a field error shows → Set Source to “Rented”, save, and check “Who does it come from?” and “Phone for pickups & returns” → Choose “Other” as a permitted use, save, and check “Describe the other permitted use”; then undo “Other” → Set Source to “Production owned” and check that the owner questions disappear → Type “Ford” as Make and “Mustang” as Model and click “Save & mark sourced” → Check that the save sends the make and model as single text values with the vehicle’s id
  - Expected: Sourcing requires a source; Rented asks for the owner and a phone, “Other” use asks for details, and Production owned needs neither; the save sends single make and model values.

- **TH-O15..O25 source for real, send for approval, approve, send to set, request and approve the return** — skipped (transportation-coordinator.spec.js:1844)
  - Where: Tools → Transportation → Action Vehicles · account: iPhone XRR
  - Setup: Creates an Action Vehicle request “[e2e-th] AV lifecycle <timestamp>” and takes it through its whole life; once sourced it can no longer be removed.
  - Steps: Open Tools → Transportation and click “Action Vehicles” in the side menu, request a vehicle with the e2e name → On “Identify”, click “Source”, set Source to “Production owned”, click “Save & mark sourced”; check “Sourced — ready to send for approval” → On “Sent for approval” → “Ready to send”, check the card is “Sourced” and click “Send for approval” → In the approver list, check “Send for approval” is disabled, pick the first person, click it, and check “Sent for approval” → Under “Awaiting approval”, check “0/1 approved”; click “Track approval”, check “0 of 1 approved” and Close → On “Pending approval”, click “Review”, then “Approve”; check “Approved — finalized” → On “Finalized”, click “Send to set” and check the toast “… On set” → Click “Request return”, pick the first person, click “Send for return approval” and check “Return 0/1” → On “Pending approval”, click “Review return”, then “Approve return”; check “Return approved”
  - Expected: A vehicle request can be sourced, sent for approval, approved and finalized, sent to set, and returned with an approved return.

- **Pick on map fills Location (Google Maps)** — skipped (transportation-coordinator.spec.js:1912)

- **remind / remove approver, comments threads, live comment patch, badges (second account)** — skipped (transportation-coordinator.spec.js:1913)

- **reject branch, re-source, finalized lock (needs an approver on a sourced record)** — skipped (transportation-coordinator.spec.js:1914)

### Coordinator — realtime, badges, MDI

- **a vehicle created here shows in the Assign drawer's vehicle list without a reload (stub)** — skipped (transportation-coordinator.spec.js:1919)
  - Where: Tools → Transportation → Ride Requests → Active tab → Assign Driver · account: iPhone XRR
  - Setup: The test makes the server list one fake approved ride, an empty driver roster and no available vehicles.
  - Steps: Open Tools → Transportation and click “Ride Requests” in the side menu → Click the “Active” tab, then “Assign Driver” on the fake ride’s card → Check that the drawer says “No vehicles are available right now.” → Click “Cancel”
  - Expected: With no vehicles, the Assign drawer says “No vehicles are available right now.” Despite the title, the test never creates a vehicle or checks a live refresh.

- **passenger ↔ coordinator realtime, other-project events ignored (second account)** — skipped (transportation-coordinator.spec.js:1975)

- **read-on-open and silent edits (second account)** — skipped (transportation-coordinator.spec.js:1976)

- **config without google_map_key → pickers show "Loading Google Maps..." (config endpoint stub)** — skipped (transportation-coordinator.spec.js:1977)

- **Places dropdown z-index behind the MDI window (Google Maps probe)** — skipped (transportation-coordinator.spec.js:1978)

### Coordinator — Request a Ride modal

- **a stop dropped on the Manage stops map rides inside the create request — no follow-up add-stop call (stub)** — bug · known bug In the Windowed view “Manage stops” opens underneath — its map is covered, so no stop can be picked,In the Windowed view “Manage stops” opens underneath — its map is covered, so no stop can be picked (transportation-coordinator.spec.js:754)
  - Where: Tools → Transportation → Ride Requests → New Request · account: iPhone XRR
  - Setup: The test catches the new ride request and any “add stop” call and answers them with success, so no ride is created.
  - Steps: Open Tools → Transportation, click “Ride Requests”, then “New Request” → Type the e2e pickup and dropoff addresses → Click “Add stops” and wait for the map in “Manage stops” → Check that the Manage stops map is on top and not covered by another layer → Click on the map once and check that the footer says “1 stop ready” → Click “Save stops” and check that the stop shows in the request (the button now reads “Manage stops”) → Click “Request Ride” → Check that the request carries the one stop with its map coordinates, as a coordinator request → Check that no separate “add stop” call follows
  - Expected: A stop picked on the map travels inside the new ride request itself; nothing adds it again afterwards. Known bug: in the Windowed view “Manage stops” opens underneath, so its map is covered and no stop can be picked.
  - Web error: Error: the Manage stops map is covered by another layer

### Coordinator — Assign drawer

- **flipping an off-duty driver on duty from the drawer confirms it and makes the row selectable (stub)** — passed (transportation-coordinator.spec.js:852)
  - Where: Tools → Transportation → Ride Requests → Active tab · account: iPhone XRR
  - Setup: The test makes the server list one fake approved ride and a roster with one off-duty driver “[e2e-th] Stub Off Duty” and no vehicles; the duty change is answered with success.
  - Steps: Open Tools → Transportation, click “Ride Requests”, then the “Active” tab → Click “Assign Driver” on the fake ride → Check that the drawer says “Nobody is on duty — flip a driver on to select them.” and the driver cannot be picked → Check that the assign button is disabled → Switch the driver’s duty switch on → Check that the toast “[e2e-th] Stub Off Duty is now on duty” shows and the change was sent as on duty → Click the driver and check that the assign button is enabled → Click “Cancel”
  - Expected: An off-duty driver cannot be picked; switching them on duty from the drawer confirms it and makes them selectable.

- **"Reassign" on Trip Detail opens "Reassign Driver" and transfers the trip to the picked driver (stub)** — passed (transportation-coordinator.spec.js:875)
  - Where: Tools → Transportation → Trips → trip page · account: iPhone XRR
  - Setup: The test makes the server list one fake assigned trip with driver “[e2e-th] Stub Driver”, a roster with that driver and “[e2e-th] Other Driver” (the only one available), and answers the reassignment with success.
  - Steps: Open Tools → Transportation, click “Trips”, then “View” on the fake trip → Check that the trip shows “[e2e-th] Stub Driver” → Click “Reassign” → In the “Reassign Driver” drawer click “[e2e-th] Other Driver” → Click “Reassign” in the drawer → Check that the toast “Driver reassigned” shows, the drawer closes and the transfer went out for the other driver
  - Expected: Reassign on a trip opens the Reassign Driver drawer and moves the trip to the picked driver.

### Coordinator — Trips and Trip Detail

- **on a trip with a driver, "En route to pickup" asks to confirm, then sends the new status and updates the header (stub)** — passed (transportation-coordinator.spec.js:1198)
  - Where: Tools → Transportation → Trips → trip page · account: iPhone XRR
  - Setup: The test makes the server list one fake assigned trip with driver “[e2e-th] Stub Driver” and answers the status change with success.
  - Steps: Open Tools → Transportation, click “Trips”, then “View” on the fake trip → Check that the status buttons are available and “Mark assigned” is disabled → Click “En route to pickup” and check the question “Change the trip status to “En route to pickup”?” → Click “No” and check that nothing was sent → Click “En route to pickup” again and click “Yes” → Check the toast “Status updated to en route pickup” and that the new status was sent → Check that the status tag now reads “en route pickup”
  - Expected: With a driver on the trip, a status button asks for confirmation; No does nothing, Yes sends the new status and the trip shows it.

- **"Send reminder" on a trip with a driver posts the reminder and confirms it (stub)** — passed (transportation-coordinator.spec.js:1230)
  - Where: Tools → Transportation → Trips → trip page · account: iPhone XRR
  - Setup: The test makes the server list one fake assigned trip with driver “[e2e-th] Stub Driver” and answers the reminder with success.
  - Steps: Open Tools → Transportation, click “Trips”, then “View” on the fake trip → Check that “Send reminder” is enabled → Click “Send reminder” → Check that the toast “Reminder sent” shows and exactly one reminder was sent
  - Expected: On a trip with a driver, Send reminder sends one reminder and confirms it.

- **Create Trip with a driver picked creates the trip, then assigns it to that driver (stub)** — passed (transportation-coordinator.spec.js:1266)
  - Where: Tools → Transportation → Trips → New Trip · account: iPhone XRR
  - Setup: The test offers one fake on-duty driver “[e2e-th] Stub Driver” and answers the new trip and its assignment with success, so no trip is created.
  - Steps: Open Tools → Transportation, click “Trips”, then “New Trip” → Type “[e2e-th] trip with a driver” as Name and the e2e pickup and dropoff addresses → Open the Driver list and pick “[e2e-th] Stub Driver” → Click “Create Trip” → Check that the trip is sent once with that driver, then assigned to that driver → Check that the drawer closes
  - Expected: Creating a trip with a driver picked creates the trip and then assigns it to that driver.

### Coordinator — Drivers

- **"Ignore & create" with licence photos lands in Incomplete profiles; "Complete details" finishes it (slot b as the member)** — failed (transportation-coordinator.spec.js:1537)
  - Web error: Test timeout of 270000ms exceeded.

### Coordinator — Permanent Allocations

- **an allocation saved with Google Places addresses turns IN USE from its draft card, then is deleted** — failed (transportation-coordinator.spec.js:1723)
  - Web error: Error: expect(received).not.toBe(expected) // Object.is equality

- **an IN USE allocation pauses and resumes; Swap Driver needs a pick, then swaps and lists "Swap History (1)" (stub)** — passed (transportation-coordinator.spec.js:1781)
  - Where: Tools → Transportation → Allocations · account: iPhone XRR
  - Setup: The test makes the server list one fake IN USE allocation “[e2e-th] stub allocation” driven by “[e2e-th] Stub Driver”, a roster with “[e2e-th] Next Driver”, and answers pause, resume and swap with success.
  - Steps: Open Tools → Transportation and click “Allocations” → Check that the fake card shows “IN USE”; click “Pause” and check “PAUSED” → Click “Resume” and check “IN USE” again (pause, then resume were sent) → Click “Swap Vehicle”, confirm with nothing picked and check “Pick a vehicle.”; click “Cancel” → Click “Swap Driver”, confirm with nothing picked and check “Pick a driver.” → Pick “[e2e-th] Next Driver”, type the reason “e2e swap” and confirm “Swap Driver” → Check that the swap was sent for the new driver → Click “Swap History (1)” and check that a driver swap is listed
  - Expected: An allocation pauses and resumes; a swap needs a pick, and a driver swap is recorded in Swap History.

### Coordinator — Shuttles

- **a route created with typed stop addresses appears as an Active card, then is deleted** — passed (transportation-coordinator.spec.js:1935)
  - Where: Tools → Transportation → Shuttles → Routes · account: iPhone XRR
  - Setup: Creates a shuttle route “[e2e-th] base <timestamp> → [e2e-th] set” with two stops whose addresses are typed by hand.
  - Steps: Open Tools → Transportation, click “Shuttles”, then “New Route” → Type the e2e From and To, and the stop names “Stop one” and “Stop two” → For each stop click its address field, type “[e2e-th] stop 1” (or 2) in the map picker and click “Use this location” → Check that each stop shows its typed address → Click “Create Route” and check that the route goes out with both stops and the 06:00–22:00 hours → Check that the new card shows “Active” and the first stop’s address
  - Expected: A route with two stops is created from typed addresses and appears as an Active card.

- **Generate runs on an assigned route confirms and opens Runs; a run steps Start Boarding → Depart → Complete and another is cancelled (stub)** — passed (transportation-coordinator.spec.js:1981)
  - Where: Tools → Transportation → Shuttles · account: iPhone XRR
  - Setup: The test makes the server list one fake route “[e2e-th] Base → [e2e-th] Set” with a driver and a vehicle, two scheduled runs, and answers run generation and every run change with success.
  - Steps: Open Tools → Transportation and click “Shuttles” → Click “Generate runs” on the fake route → Check the toast “Runs generated for [e2e-th] Base → [e2e-th] Set.” and that the Runs tab opens → On “[e2e-th] run one” click “Start Boarding” and check “BOARDING” → Click “Depart” and check “IN-TRANSIT”; click “Complete” and check “COMPLETED” with no action left → On “[e2e-th] run two” click “Cancel” and check “CANCELLED” → Check that the statuses sent were boarding, in transit, completed and cancelled
  - Expected: Generating runs for a staffed route confirms and opens Runs; a run steps through boarding, departure and completion, and a run can be cancelled.

- **a route disables and re-enables, edits with its values prefilled, and deletes after "Delete route?" (stub)** — passed (transportation-coordinator.spec.js:2011)
  - Where: Tools → Transportation → Shuttles · account: iPhone XRR
  - Setup: The test makes the server list one fake route “[e2e-th] Base → [e2e-th] Set” and answers every change and the delete with success.
  - Steps: Open Tools → Transportation and click “Shuttles” → Click “Disable” on the route and check “Inactive”; click “Enable” and check “Active” → Click “Edit” and check that “Edit Shuttle Route” shows From “[e2e-th] Base” and To “[e2e-th] Set” → Change From to “[e2e-th] Base edited” and click “Save Changes” → Check that the saved name is “[e2e-th] Base edited → [e2e-th] Set” → Click the delete button on the card and confirm “Delete route?” with “Delete” → Check that the card disappears and the delete was sent
  - Expected: A route can be disabled and enabled, edited with its values filled in, and deleted after confirming.

- **an end time before the start time is refused when creating a route @smoke** — passed (transportation-coordinator.spec.js:2052)
  - Where: Tools → Transportation → Shuttles → New Route · account: iPhone XRR
  - Steps: Open Tools → Transportation, click “Shuttles”, then “New Route” → Type the e2e From and To → Change the end time from 22:00 to 05:00 (before the 06:00 start) → Click “Create Route” → Check that “End time must be after the start time…” shows and nothing is created → Click “Cancel”
  - Expected: A route whose end time is before its start time is refused.

### Coordinator — Live Map

- **selecting a ride under Active Rides shows "Tracking ride" with Clear, and Clear lets it go (stub)** — passed (transportation-coordinator.spec.js:2129)
  - Where: Tools → Transportation → Live Tracking (titled “Live Map”) · account: iPhone XRR
  - Setup: The test makes the server list one fake assigned ride for “[e2e-th] Pax Live” and no live drivers.
  - Steps: Open Tools → Transportation and click “Live Tracking” → Click the “Active Rides” tab → Check that “Tracking ride” is not shown yet → Click the “[e2e-th] Pax Live” ride → Check that “Tracking ride” shows and the ride is marked selected → Click “Clear” and check that “Tracking ride” disappears and the ride is no longer selected
  - Expected: Selecting a ride tracks it under “Tracking ride”; Clear lets it go.

### Coordinator — Action Vehicles

- **"Pick on map": Use this location stays disabled until a place is entered, then fills Location @smoke** — passed (transportation-coordinator.spec.js:2562)
  - Where: Tools → Transportation → Action Vehicles → Requested vehicles → Request a vehicle · account: iPhone XRR
  - Steps: Open Tools → Transportation, click “Action Vehicles”, then “Request a vehicle” → Check that Location is empty, then click “Pick on map” → Check that “Use this location” is disabled → Type “[e2e-th] Stage 9” in the map search → Check that “Use this location” is enabled and click it → Check that Location now reads “[e2e-th] Stage 9” → Click “Cancel” (nothing is requested)
  - Expected: The map picker cannot confirm without a place; once a place is entered, it fills the Location field.

- **an approver rejects with a reason: Review → Reject → "Reject request" → Done sends it back to transport (stub)** — passed (transportation-coordinator.spec.js:2673)
  - Where: Tools → Transportation → Action Vehicles → Pending approval · account: iPhone XRR
  - Setup: The test makes the server list one fake record “[e2e-th] stub AV to reject” awaiting your approval, and answers the rejection with success.
  - Steps: Open Tools → Transportation, click “Action Vehicles”, then “Pending approval” → Click “Review” on the fake record → Click “Reject” and check the “Reject request” explanation → Type “e2e stub reason” in “Reason (optional)” and click “Done” → Check the toast “Sent back to transport” and that the reason was sent → Open “Sent for approval” → “Ready to send” and check “Rejected: e2e stub reason” on the card
  - Expected: An approver rejects with a reason; the record goes back to transport and shows the reason.

- **a rejected record shows its reason and is re-sourced from Sent for approval → Ready to send (stub)** — passed (transportation-coordinator.spec.js:2713)
  - Where: Tools → Transportation → Action Vehicles → Sent for approval → Ready to send · account: iPhone XRR
  - Setup: The test makes the server list one fake rejected record “[e2e-th] stub AV to re-source” and answers the sourcing with success.
  - Steps: Open Tools → Transportation, click “Action Vehicles”, then “Sent for approval” and “Ready to send” → Check that the card shows “Rejected: e2e stub reason” → Click “Re-source” → Set Source to “Production owned”, type Make “Ford” and Model “Mustang” → Click “Save & mark sourced” → Check the toast “Sourced — ready to send for approval” and that the save carried the record and the make
  - Expected: A rejected record shows why and can be sourced again.

- **editing a finalized record shows the lock notice and saves only the dates (stub)** — passed (transportation-coordinator.spec.js:2823)
  - Where: Tools → Transportation → Action Vehicles → Finalized · account: iPhone XRR
  - Setup: The test makes the server list one fake finalized record “[e2e-th] stub AV finalized” that you requested, and answers the save with success.
  - Steps: Open Tools → Transportation, click “Action Vehicles”, then “Finalized” → Click “Edit” on the fake record → Check the notice “This vehicle is finalized” and “The car and terms are locked. Only the rehearsal and shooting dates can be changed.” → Check that the rehearsal and shooting dates can be edited and the display name cannot → Click “Save changes” and check the toast “Saved” → Check that only the two dates and the time zone were sent
  - Expected: A finalized record is locked except for its rehearsal and shooting dates, and saving sends only those.

- **the approval tracker reminds a pending approver and removes another after confirming (stub)** — passed (transportation-coordinator.spec.js:2789)
  - Where: Tools → Transportation → Action Vehicles → Sent for approval → Awaiting approval · account: iPhone XRR
  - Setup: The test makes the server list one fake record “[e2e-th] stub AV awaiting approval” with two pending approvers (“[e2e-th] Approver One”, “[e2e-th] Approver Two”) and answers the reminder and the removal with success.
  - Steps: Open Tools → Transportation, click “Action Vehicles”, then “Sent for approval” and “Awaiting approval” → Click “Track approval” on the fake record and check “0 of 2 approved” → Click “Remind” on Approver One and check the toast “Reminder sent to [e2e-th] Approver One” → Click the remove button on Approver Two and confirm “Remove [e2e-th] Approver Two as an approver?” with “Remove” → Check the toast “Removed [e2e-th] Approver Two” → Check that the reminder and the removal named the right approvers
  - Expected: The approval tracker reminds a pending approver and removes another after confirmation.

- **"Send for comments" lists project users but never yourself, and sends the picked people with the first message (stub)** — passed (transportation-coordinator.spec.js:2830)
  - Where: Tools → Transportation → Action Vehicles → Requested vehicles · account: iPhone XRR
  - Setup: The test makes the server list one fake requested record “[e2e-th] stub AV for comments” that you requested, and answers “Send for comments” with success.
  - Steps: Open Tools → Transportation, click “Action Vehicles”, then “Requested vehicles” → Click “Send for comments” on the fake record → Check that project users are listed but you are not, and that the send button is disabled → Click Lava Device (or the first person listed) → Type “[e2e-th] first message” as the first message and click “Send for comments” → Check the toast “Sent for comments” and that one person and the message were sent
  - Expected: Send for comments never offers yourself, needs at least one person, and sends the picked people with the first message.

### Coordinator — realtime, badges, MDI

- **MDI: the Google Places suggestions for a pickup address sit above the tool window @smoke** — passed (transportation-coordinator.spec.js:2932)
  - Where: Tools → Transportation → Ride Requests → New Request (Windowed view) · account: iPhone XRR
  - Steps: Open Tools → Transportation in the Windowed view, click “Ride Requests”, then “New Request” → Type “Connaught Place New Delhi” in the pickup address → Wait for Google’s suggestions → Check that the first suggestion is on top (not covered by the tool window) and record both layers → Press Escape
  - Expected: The address suggestions show above the tool window, so they can be clicked.

### Coordinator — two users (slot b): passenger ↔ coordinator realtime and badges

- **a ride slot b requests appears in the coordinator's Pending tab and raises the Ride Requests badge without a reload** — skipped (transportation-coordinator.spec.js:3095)
  - Where: Tools → Transportation → Ride Requests (iPhone XRR) and Ride Requests, titled “My Rides” (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Lava Device opens the hub as a passenger in a second browser.
  - Steps: As iPhone XRR open Ride Requests, the “Pending” tab, and note the Ride Requests count in the side menu → As Lava Device open Ride Requests and click “Call a Driver” → Type the e2e pickup “[e2e-th] live ride <timestamp> pickup”, dropoff and notes, then click “Call a Driver” → Check that Lava Device’s card says “Waiting for coordinator approval” → Without reloading, check that the ride appears in iPhone XRR’s Pending tab → Check that iPhone XRR’s Ride Requests count in the side menu goes up
  - Expected: A passenger’s new ride appears live in the coordinator’s Pending tab and raises the Ride Requests badge.

- **opening Ride Requests clears its badge while Trips leaves it alone; slot b editing its pending ride does not raise it again** — skipped (transportation-coordinator.spec.js:3118)
  - Where: Tools → Transportation (iPhone XRR) and Ride Requests, titled “My Rides” (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Continues with the ride Lava Device requested in TH-D21/S01.
  - Steps: As iPhone XRR open the hub and check that Ride Requests shows an unread count → Click “Trips”, wait 3 seconds and check that the Ride Requests count did not change → Click “Ride Requests” and check that its count goes to zero → As Lava Device click “Edit” on the ride, change the notes to “… notes edited”, click “Save Changes” and check “Ride updated” → Wait 10 seconds and check that iPhone XRR’s Ride Requests count is still zero
  - Expected: Opening Ride Requests clears its badge while opening Trips does not; editing a pending ride does not raise the badge again.

- **when the coordinator rejects slot b's ride, b's card turns Rejected without a reload** — skipped (transportation-coordinator.spec.js:3144)
  - Where: Tools → Transportation → Ride Requests (iPhone XRR) and Ride Requests, titled “My Rides” (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Continues with the ride Lava Device requested in TH-D21/S01; Lava Device’s My Rides stays open.
  - Steps: As iPhone XRR open Ride Requests, “Pending”, and find Lava Device’s ride → Click “Reject”, type “e2e cleanup” as the reason and click “Reject Request” → Without reloading, check that Lava Device’s card no longer says “Waiting for coordinator approval” → Check that Lava Device’s card shows “Rejected”
  - Expected: When the coordinator rejects a ride, the passenger’s card turns Rejected live.

### Coordinator — two users (slot b): Action Vehicles comments

- **slot b's comment appears in slot a's open thread without a reload; with the thread closed it marks Comments unread and raises the Action Vehicles badge, cleared on open** — skipped (transportation-coordinator.spec.js:3203)
  - Where: Tools → Transportation → Action Vehicles (iPhone XRR and Lava Device) · account: iPhone XRR and Lava Device
  - Steps: Note: this test is a placeholder and is always skipped; the steps below are its intent only → As iPhone XRR request a vehicle, send it for comments to Lava Device and keep the thread open → As Lava Device (with the Transportation Post right) open the record’s Comments and add a comment → Check that iPhone XRR’s open thread shows the comment without a reload and the Action Vehicles count goes up
  - Expected: Not automated (marked as to-do, so it never runs). Intended: a comment from another person appears live in an open thread and raises the Action Vehicles badge.

### Transportation Hub — passenger: Track Driver (stubbed ride)

- **an assigned ride offers "Track driver": the page shows the driver, the vehicle and the last ping, and Back returns (stub)** — passed (transportation.spec.js:715)
  - Where: Tools → Transportation → Ride Requests (titled “My Rides”) · account: Lava Device
  - Setup: Runs as the account that opens the hub as a passenger (Lava Device while iPhone XRR is a coordinator). The test makes the server list one fake ride that is already assigned to “[e2e-th] Stub Driver” with the vehicle “[e2e-th] Stub Van”, and answers the driver’s last position with a fixed point.
  - Steps: Open Tools → Transportation as the passenger and wait for the passenger menu (“My Trips”, “Ride Requests”) → Click “Ride Requests” in the side menu → On the fake ride’s card click “Track driver” → Check that “Track Driver” opens with Driver “[e2e-th] Stub Driver” → Check that Vehicle shows “[e2e-th] Stub Van · E2E-0001” → Check that Last ping shows “28.61390, 77.20900” → Click “Back” and check that “My Rides” shows again
  - Expected: An assigned ride offers “Track driver”; the tracking page names the driver and the vehicle and shows the last reported position; Back returns to My Rides.

### Transportation Hub — two users (slot b)

- **granting, then revoking, slot b's Transportation Post right in User Management switches b's open hub between the passenger and coordinator menus without a reload** — passed (transportation.spec.js:803)
  - Where: Settings → Admin Settings → User Management (iPhone XRR) and Tools → Transportation (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Lava Device opens the hub in a second browser; iPhone XRR reads Lava Device’s current Transportation rights first. If the Post switch is locked because Lava Device is an admin, the test removes the admin flag for the duration of the test.
  - Steps: As iPhone XRR open Settings → Admin Settings → User Management and find Lava Device’s row → Open Lava Device’s posting rights, choose the Tools section and find the “Transportation” row → As Lava Device open Tools → Transportation and check its side menu (passenger: “My Trips”, “Ride Requests”) → As iPhone XRR switch Lava Device’s Transportation View and Post on → Without reloading, check that Lava Device’s side menu changes to the ten coordinator pages and “Dashboard” shows → As iPhone XRR switch the Post right back off → Without reloading, check that Lava Device’s side menu is back to “My Trips” and “Ride Requests”
  - Expected: Changing another member’s Transportation Post right in User Management switches that member’s open hub between the passenger and coordinator menus live, both ways.

- **adding slot b as a temporary driver switches b's open hub to the driver menu with the "Temp Driver" tag; deleting the record switches it back** — passed (transportation.spec.js:851)
  - Where: Tools → Transportation → Drivers (iPhone XRR) and Tools → Transportation (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: Lava Device opens the hub in a second browser as a passenger. Any driver record left for Lava Device by an earlier run is deleted first.
  - Steps: As Lava Device open Tools → Transportation and check the passenger menu without a “Temp Driver” tag → As iPhone XRR open Drivers and click “New Driver” → Pick Lava Device in “Select user”, type a phone “9999999999” and a licence number “E2E-<timestamp>”, click “Create” → Check that Lava Device’s driver card shows “Temp” → Without reloading, check that Lava Device’s side menu changes to “My Trips”, “My Map”, “Assigned Rides”, “Ride Requests” and the “Temp Driver” tag shows → As iPhone XRR click the delete button on Lava Device’s driver card and confirm “Remove driver?” with “Delete” → Without reloading, check that Lava Device’s menu is back to “My Trips” and “Ride Requests” and the tag is gone
  - Expected: Adding a member as a temporary driver switches their open hub to the driver menu with the “Temp Driver” tag; deleting the driver record switches it back.

### Transportation Hub — two users (slot b): b as a temporary driver

- **My Map: going on duty sends a location ping at once and shows the sharing notice; going off duty stops the pings (pings stubbed)** — passed (transportation.spec.js:935)
  - Where: Tools → Transportation → My Map (as a driver) · account: Lava Device
  - Setup: Before these driver cases, iPhone XRR adds Lava Device as a temporary driver in Drivers. The test catches the location updates the page sends and answers them with success, so no position is stored.
  - Steps: As Lava Device open Tools → Transportation and wait for the driver menu → Click “My Map”; if the duty switch is on, switch it off first → Switch duty on → Check that the toast “You are now on duty” shows and a location update is sent at once with the driver and a latitude → Check that the notice about sharing your live location shows → Switch duty off and check the toast “You are now off duty” → Wait 5 seconds and check that no further location update is sent and the notice is gone
  - Expected: Going on duty sends a location update straight away and explains that the location is shared; going off duty stops the updates.

- **Assigned Rides: Start Trip → Arrived at Pickup → Start Transit → Arrived at Dropoff → Complete moves the ride to Past (stub)** — passed (transportation.spec.js:979)
  - Where: Tools → Transportation → Assigned Rides (titled “My Rides”, as a driver) · account: Lava Device
  - Setup: Lava Device is a temporary driver (added by iPhone XRR before these cases). The test makes the server list one fake ride assigned to Lava Device and answers every status change with success.
  - Steps: As Lava Device open Tools → Transportation and wait for the driver menu → Click “Assigned Rides” and find the fake ride “[e2e-th] stub driver pickup” → Click “Start Trip” and check the toast “Start Trip — done” → Click “Arrived at Pickup”, then “Start Transit”, then “Arrived at Dropoff”, checking each “… — done” toast → Click “Complete” and check the toast “Complete — done” → Check that the statuses sent were en route to pickup, at pickup, in transit, at dropoff and completed → Check that the Active tab now says “No active rides assigned to you.” → Click “Past” and check that the ride is listed there
  - Expected: A driver walks an assigned ride through every step to Complete, one button at a time, and the finished ride moves to Past.

- **driver My Trips lists only in-flight trips, and says so when the account has no driver record (stub)** — passed (transportation.spec.js:1018)
  - Where: Tools → Transportation → My Trips (as a driver) · account: Lava Device
  - Setup: Lava Device is a temporary driver. First the test makes the server list five fake trips (assigned, in transit, completed, cancelled, pending); then, in a fresh session, it makes the drivers list come back empty.
  - Steps: As Lava Device open Tools → Transportation and wait for the driver menu on “My Trips” → Check that the banner says “2 trips” and two “View Detail →” links show (only the assigned and in-transit trips) → Close that browser and open the hub again with the drivers list faked as empty → Check that My Trips says “No driver record found for your account.”
  - Expected: A driver’s My Trips shows only trips in progress (assigned through drop-off) and hides finished, cancelled and pending ones; without a driver record it says so.

- **on the driver's own trip, Trip Detail hides Reassign and Send reminder (stub)** — passed (transportation.spec.js:1046)
  - Where: Tools → Transportation → My Trips → View Detail (as a driver) · account: Lava Device
  - Setup: Lava Device is a temporary driver. The test makes the server list one fake assigned trip whose driver is Lava Device, and answers its detail, timeline and events.
  - Steps: As Lava Device open Tools → Transportation and wait for the driver menu → On My Trips click “View Detail →” on the fake trip → Check that the trip page opens with “Trip #…” and Assigned Driver showing Lava Device → Check that there is no “Send reminder” button → Check that there is no “Reassign” (or “Assign”) button
  - Expected: On a trip the signed-in driver is driving, the trip page hides Reassign and Send reminder.

### Coordinator — Drivers

- **"Ignore & create" saves a driver without phone or licence into Incomplete profiles; "Complete details" asks for them and completes it (slot b as the member)** — passed (transportation-coordinator.spec.js:1570)
  - Where: Tools → Transportation → Drivers · account: iPhone XRR
  - Setup: Uses Lava Device (the second test account) as the member to add; any driver record left for it is deleted first.
  - Steps: Open Tools → Transportation, click “Drivers”, then “New Driver” → Pick Lava Device in “Select user” and clear the phone → Click “Ignore & create” and check the toast “Driver added — details pending…” → Check that “Profiles needing details” lists Lava Device with the phone missing → Click “Complete details”, then “Save details”, and check “Phone is required” → Type phone “9999999999” and a licence number, click “Save details” → Check the toast “Details completed — profile is now full” and that the row leaves the list
  - Expected: Ignore & create saves a driver without phone and licence into Incomplete profiles; Complete details requires the phone and then completes the profile.

- **licence photos: front and back upload and can be removed; photos added before "Ignore & create" show in Complete details (slot b as the member)** — skipped (transportation-coordinator.spec.js:1605)
  - Where: Tools → Transportation → Drivers · account: iPhone XRR
  - Setup: Uses Lava Device (the second test account) as the member to add; any driver record left for it is deleted first.
  - Steps: Open Tools → Transportation, click “Drivers”, then “New Driver” and pick Lava Device → Add a front licence photo, wait for “Uploading…” to finish, then remove it and check “Add front” returns → Add the front photo again and a back photo; wait for both uploads and check both remove buttons → Click “Ignore & create” and check the toast “Driver added — details pending…” → Click “Complete details” on Lava Device’s row → Check that both licence photos are already there
  - Expected: Licence photos upload and can be removed, and photos added before Ignore & create carry over to Complete details.

### Coordinator — Permanent Allocations

- **an allocation saved with Google Places addresses and one passenger turns IN USE from its draft card, then is deleted** — skipped (transportation-coordinator.spec.js:1790)
  - Where: Tools → Transportation → Allocations · account: iPhone XRR
  - Setup: Creates a permanent allocation “[e2e-th] places <timestamp>” as a draft, with Google suggestions for every address and yourself as the passenger. Leftover e2e drafts from earlier runs are deleted first.
  - Steps: Open Tools → Transportation, click “Allocations”, then “New Permanent Allocation” → Type the e2e title; pick “Connaught Place New Delhi” as pickup and “India Gate New Delhi” as dropoff from Google’s suggestions → Click “Add Passenger” and pick yourself in “Select user” → For the passenger’s pickup and dropoff click “Map”, pick the same places from the suggestions and click “Use this location” → Click “Cancel”, then “Save draft”, and check that the draft carries real coordinates and one passenger → Open “Drafts” and check that the card shows “DRAFT” → Click “Create Allocation” on the card and check that it is accepted as active → Open “Active” and check that the card shows “IN USE”
  - Expected: A draft whose addresses came from Google suggestions, with a passenger, turns into an IN USE allocation from its card.

### Coordinator — realtime, badges, MDI

- **hub events from another project are ignored — needs a Transportation event from a second project that both accounts share; the hub's socket events cannot be injected from the page** — notrun (transportation-coordinator.spec.js:3055)
  - Where: Tools → Transportation · account: iPhone XRR
  - Steps: Note: this test is a placeholder and is always skipped; the steps below are its intent only → Keep the hub open in the sandbox project → Make a Transportation change in another project you share with a second account → Check that nothing in the sandbox hub refreshes or changes
  - Expected: Not automated (marked as to-do, so it never runs): it needs an event from a second shared project, which the page cannot produce. Intended: changes in other projects never refresh this project’s hub.

- **a project config without google_map_key leaves the pickers on "Loading Google Maps..." — the config (with the key) is persisted in the encrypted store and rehydrates before any request, so a stubbed config never reaches the pickers (same reason MAP-N01 skips)** — notrun (transportation-coordinator.spec.js:3056)
  - Where: Tools → Transportation → any address picker · account: iPhone XRR
  - Steps: Note: this test is a placeholder and is always skipped; the steps below are its intent only → Make the app’s settings come back without a Google Maps key → Open an address picker (for example New Request) → Check that it shows “Loading Google Maps...” and no address field
  - Expected: Not automated (marked as to-do, so it never runs): the app keeps its saved settings, key included, so a faked settings reply never reaches the pickers. Intended: without a Google Maps key, the address pickers say “Loading Google Maps...”.

## Sheet bugs (84)

- **TRANS-01** In windowed view the hub content is taller than its window — Low · Web: Ready to retest, Android: Not affected
  - Steps: 1. Switch to windowed view. 2. Open Transportation.
  - Expected: The hub fits the window; only its own lists scroll.
  - Actual: Confirmed: in Windowed view the window body scrolls (1000 px of content in a 679 px body) and an inner area scrolls too (1141 px in 425 px), so there are two nested scrollbars.

- **TRANS-02** "More filters" button does nothing — Low · Web: Ready to retest, Android: Not affected
  - Steps: 1. As a coordinator, open the Dashboard. 2. Click the funnel "More filters" button.
  - Expected: Filter options open.
  - Actual: Clicking the funnel only shows its "Filter" tooltip. No filter options open.

- **TRANS-03** "Completed Today" counts every completed and cancelled ride, not just today's — Medium · Web: Ready to retest, Android: Not checked
  - Steps: 1. Have rides completed on earlier days, or cancelled. 2. Look at the "Completed Today" card.
  - Expected: Only rides completed today are counted.
  - Actual: On 5 Nov the card shows 4: three completed and one cancelled ride, all from 19 Jun 2026. The Dashboard's own Completed Today card shows 0 for the same project.

- **TRANS-04** Create Trip accepts a pickup time in the past — Medium · Web: Ready to retest, Android: Not affected
  - Steps: 1. Pick tomorrow's date. 2. Set Pickup time to 00:05. 3. Change the date back to today. 4. Fill the rest and create the trip.
  - Expected: Blocked: the pickup time is in the past.
  - Actual: At 19:33 on 5 Nov, a trip with pickup 00:05 on 5 Nov was created. The list shows "Today, 12:05 AM" and the trip detail shows "05 Nov 2026, 00:05". The picker only blocks past times while the date is already today.

- **TRANS-05** Shuttle end time can be before the start time — Medium · Web: Ready to retest, Android: Not checked
  - Steps: 1. New Route, fill From, To and two stops. 2. Set Start time 18:00 and End time 08:00. 3. Create Route.
  - Expected: A validation error.
  - Actual: The route saved with no warning and the card shows 18:00–08:00. The form has no start/end rule, and the server accepted it.

- **TRANS-06** Action Vehicles: past dates allowed for both day fields — Medium · Web: Open, Android: Not checked
  - Steps: 1. Pick past dates for the rehearsal and shooting days. 2. Submit.
  - Expected: Past dates are blocked.
  - Actual: No day is disabled in either date picker. With today 5 Nov, a request with rehearsal 1 Nov and shooting 2 Nov was accepted: "Vehicle requested — sent to transport".

- **TRANS-07** Passenger ride request can be submitted for a time already passed — Low · Web: Ready to retest, Android: Not affected
  - Steps: 1. As a passenger, open Call a Driver and fill the addresses. 2. Choose Schedule Time and pick the current minute. 3. Leave the form open until that minute has passed. 4. Press Call a Driver.
  - Expected: Blocked on submit.
  - Actual: At 11:35 on 6 Nov a ride scheduled for 11:34 was accepted and shows that passed time. There is no time check on submit. The picker itself is fine: going from tomorrow 00:05 back to today corrected the time to now.

- **TRANS-08** Address suggestions may appear behind the Transportation window — Medium · Web: Not affected, Android: Not affected
  - Steps: 1. In windowed view, type an address in any address field.
  - Expected: The suggestions list shows above the window.
  - Actual: Not reproduced on web: in New Trip inside a Transportation window, the suggestions list shows above the window and can be clicked.

- **TRANS-09** An ASAP ride request shows "SCHEDULED PICKUP <creation time>" — Medium · Web: Not affected, Android: In progress
  - Steps: 1. As a coordinator, create a ride request without a scheduled time (ASAP). 2. Look at the request card.
  - Expected: The card shows ASAP.
  - Actual: The card showed "SCHEDULED PICKUP" with the time the request was created.

- **TRANS-10** Passenger's Transportation tile counts a rejected ride as unread although My Rides was open when it was rejected — Medium · Web: Open, Android: In progress
  - Steps: 1. As a passenger, raise a ride request and stay on My Rides. 2. As a coordinator on another device, reject it with a reason. 3. On the passenger device, watch the card, then go back to Tools and read the Transportation tile.
  - Expected: The card turns "Rejected" live, and the tile shows no unread count for this rejection (ZL-20017 / ZL-19283).
  - Actual: The card turns "Rejected" live, but the tile count goes up by 1 (2 → 3). It only drops after My Rides is opened again. Same result on a second try.

- **TRANS-11** Address search ignores the device location ("Eiffel Tower" suggests a place in India) — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. With the device in Paris, open Call a driver. 2. Type "Eiffel Tower" in Pickup, then "Louvre Museum".
  - Expected: Suggestions near the device come first.
  - Actual: The first suggestion for "Eiffel Tower" is in India; "Louvre Museum" often shows no suggestions. Testers had to use the map-tap fallback (also seen on 21 Oct).

- **TRANS-12** Edit ride shows "Schedule for later" OFF for a scheduled ride — Low · Web: Not affected, Android: Not affected
  - Steps: 1. Raise a ride with Schedule for later ON and a pickup time. 2. Open the ride's menu, tap Edit.
  - Expected: The Schedule for later toggle is ON with the saved time.
  - Actual: The toggle shows OFF. Saving keeps the scheduled time (no data loss). Also seen on 21 Oct.

- **TRANS-13** Rejected ride detail shows the rejection time under "APPROVED" — Low · Web: Ready to retest, Android: In progress
  - Steps: 1. Have a ride request rejected by a coordinator. 2. Open it with View Details.
  - Expected: No "APPROVED" time for a rejected ride (or a "REJECTED" label).
  - Actual: The "APPROVED" cell holds the time it was rejected.

- **TRANS-14** Action Vehicle Source form: validation banner is at the very bottom and nothing scrolls to it — Low · Web: Not affected, Android: Not checked
  - Steps: 1. As a coordinator, open Source on a requested Action Vehicle. 2. Leave a required field empty and tap Save.
  - Expected: The user sees what is missing (scroll to the banner or the first bad field).
  - Actual: The banner appears at the bottom of the long form, off screen; the form does not scroll.

- **TRANS-15** Rights removed while Transportation is open: the user stays inside — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Open Transportation as a coordinator on device B. 2. On device A (admin), turn off B's Transportation posting right, then the view right.
  - Expected: Like Tasks and Recce, B is sent back to Tools when the view right goes, and loses coordinator tabs at once when the posting right goes.
  - Actual: After posting OFF, coordinator tabs stay for ~15 s and switch on the next tab tap. After view OFF, B stays inside My Rides. Back on Tools the tile is hidden.

- **TRANS-16** A user without view access still gets the whole Transportation tool — High · Web: Ready to retest, Android: Not checked
  - Steps: 1. Remove view access on Transportation for a user. 2. Open the tool on desktop (e.g. a window left open, or its path).
  - Expected: Only the 'no access' notice; no data loaded, no Raise Pickup Request.
  - Actual: The notice is drawn above the normal Requests page: request rows, Raise Pickup Request and the side nav all work, and the VM loads crew, vehicles and trips regardless of canView.

- **TRANS-17** Coordinator's Update on a driver record erases documents/licence the driver uploaded meanwhile — High · Web: Not checked, Android: Not checked
  - Steps: 1. Coordinator opens a driver's record. 2. The driver uploads a document (or replaces the licence) from their phone. 3. Coordinator picks a vehicle and clicks Update.
  - Expected: The driver's new document/licence is kept.
  - Actual: On driver:updated the open record refreshes only vehicle (self) and empty licence slots; documents keep the stale list. Update always sends documents and license_picture, so the server is overwritten with the old (empty) list.

- **TRANS-18** Licence-change requests crash parsing when license_picture is a list — High · Web: Not affected, Android: Not checked
  - Steps: 1. A driver submits a licence change (server stores license_picture as [{media, caption:'Front'}, ...]). 2. Coordinator opens Drivers.
  - Expected: The request appears in 'Licence changes waiting'.
  - Actual: parseLicenceRequest reads license_picture with jsonPrimitive, which throws IllegalArgumentException for an array; the exception escapes licenceRequests() and the queue never loads (uncaught in the VM coroutine).

- **TRANS-19** Switching request tabs quickly can show the wrong tab's requests — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Open Pickup requests on a slow connection. 2. Click Completed before Pending has loaded.
  - Expected: Completed tab shows completed requests.
  - Actual: Whichever answer arrives last wins: the late Pending list replaces the Completed rows while the Completed tab is highlighted. Same pattern for Allocations and My assignments.

- **TRANS-20** An open trip is not refreshed when another device changes it — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Coordinator A opens a pending trip. 2. Coordinator B approves it on the web. 3. A looks at the open dialog.
  - Expected: Dialog reflects the new status (or closes); Approve/Reject disappear.
  - Actual: The list reloads, but the dialog keeps the stale Pending trip with Approve/Reject (and stale passengers/driver status); acting re-sends an update over B's change.

- **TRANS-21** Driver starting a trip (trip:request:inprogress) does not update other screens — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Coordinator watches Assigned. 2. Driver starts the trip on their phone.
  - Expected: The trip leaves Assigned/appears under In progress without Refresh.
  - Actual: No reaction: transportation:trip:request:inprogress is not in TRANSPORT_SYNC_KINDS.

- **TRANS-22** Esc does not close transport dialogs — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Open Raise Pickup Request, then Add passenger. 2. Press Esc.
  - Expected: The top dialog (Add passenger) closes.
  - Actual: Nothing happens; no transport dialog responds to Esc.

- **TRANS-23** Licence change is approved without seeing the submitted licence — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Coordinator opens Drivers with a pending licence change.
  - Expected: Front/back pictures shown before Approve/Reject.
  - Actual: The card lists only the driver's name with Approve/Reject; LicenceRequest.licencePicture is parsed but never displayed.

- **TRANS-24** A failed tab load leaves the previous tab's rows under the new tab — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Open Pending (rows shown). 2. Click Completed while the server fails.
  - Expected: Error and an empty/error Completed list.
  - Actual: Error toast, but the Pending rows stay listed under Completed (trips = rows ?: trips). Same for Allocations and My assignments.

- **TRANS-25** Editing a passenger moves them to the end of the list — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Raise a request with P1 then P2. 2. Edit P1's time; Update.
  - Expected: Order stays P1, P2.
  - Actual: P1 moves to the end (P2, P1).

- **TRANS-26** Phone numbers without a country code are accepted (vehicle owner, driver details) — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Add vehicle with owner number 9876543210, no country code; save. 2. Driver: Fill in details with phone but no code; Update.
  - Expected: Refused: 'enter country code and phone number'.
  - Actual: Both saved without a country code (and a code without a number is also accepted).

- **TRANS-27** Removing a temp driver reports success even when their private car was not deleted — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Unassign a temp driver whose private car delete fails.
  - Expected: Error shown; the car is not silently orphaned.
  - Actual: 'Temporary driver removed' toast; the deleteVehicles failure is ignored and the private car stays in the fleet.

- **TRANS-28** Save as draft skips the end-before-start check — Low · Web: Not affected, Android: Not checked
  - Steps: 1. New allocation: start in 3 days, end tomorrow. 2. Save as draft.
  - Expected: Refused 'end date cannot be before start date'.
  - Actual: Draft saved.

- **TRANS-29** Allocation dates: start in the past and end on the start day are accepted — Low · Web: Not affected, Android: Not checked
  - Steps: 1. New allocation with start two days ago; Submit. 2. New allocation with end = start; Submit.
  - Expected: Past start unavailable; end at least start + 1 day.
  - Actual: Both saved.

- **TRANS-30** Track driver is a one-off browser link, not a live map — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Open an in-progress trip; Track driver.
  - Expected: Live driver pin that moves (driver:location:update).
  - Actual: Opens Google Maps in the browser at the last stored position; location updates are not subscribed, so it is only as fresh as the last list reload. (Deliberate, per the code comment.)

- **TRANS-31** Assign Driver says "Nobody is on duty" while the only driver is on duty — Low · Web: Open, Android: Not checked
  - Steps: 1. Have every on-duty driver already on a trip. 2. Open a trip and click Assign.
  - Expected: The hint says the on-duty drivers are all on trips.
  - Actual: The hint says "Nobody is on duty — flip a driver on to select them.", while Vivek Mishra's row shows On duty, ON TRIP and a switched-on toggle, so there is nothing to flip. The Drivers page counts him as Available at the same moment.

- **TRANS-32** Trip Timeline stays empty after a full driver run — Medium · Web: Open, Android: Not affected
  - Steps: 1. Create a trip and assign a driver. 2. Run it on the driver's phone: Start Trip, Arrived at Pickup, Start Transit, Arrived at Dropoff, Complete. 3. Open the trip on web and reload.
  - Expected: The Timeline lists each status change with its time.
  - Actual: "No timeline entries yet." on the Completed trip, before and after a reload, although the status followed every step live.

- **TRANS-33** Driver's Trips card is titled with the coordinator's name — Low · Web: Not affected, Android: In progress
  - Steps: 1. As a coordinator, create a trip titled "[e2e-th] past pickup check" for passenger "e2e passenger" and assign a driver. 2. On the driver's phone, open Trips.
  - Expected: The card shows the trip title or the passenger, so the driver knows who to collect.
  - Actual: The card title is "iPhone XRR", the coordinator who created the trip. Neither the title nor the passenger is on the card.

- **TRANS-34** Driver map still says "Sharing" after Go Off Duty — Medium · Web: Not affected, Android: In progress
  - Steps: 1. As a driver, open Map while on duty. 2. Tap Go Off Duty and confirm. 3. Read the status strip at the bottom.
  - Expected: The strip stops saying Sharing once the driver is off duty.
  - Actual: The button changes to Go On Duty and the coordinator sees Off duty live, but the strip still says "Sharing" with the green icon.

- **TRANS-35** Removing a temp driver leaves the phone on driver tabs — Low · Web: Not affected, Android: In progress
  - Steps: 1. Keep Transportation open on the driver's phone. 2. As a coordinator, delete that temp driver in Drivers.
  - Expected: The phone drops to the passenger tabs, as it does when Post changes.
  - Actual: About 100 s later the phone still showed Map, Trips, Assigned Rides and Ride Requests, with the duty button gone. It only moved on when a Post change arrived. Adding the driver role, and Post on or off, all updated within seconds.

- **TRANS-36** Coordinator's ride card shows the request time, not the scheduled pickup — Medium · Web: Not affected, Android: In progress
  - Steps: 1. As a passenger, schedule a ride for tomorrow 18:00. 2. As a coordinator on Android, open Rides and read the card. 3. Open the card's details.
  - Expected: The card shows the scheduled pickup, Nov 07 18:00.
  - Actual: The card shows Nov 06, 2026 11:35, the moment the ride was requested, and still does after Refresh. Request Details on the same ride shows Scheduled Nov 07, 2026 6:00 pm, so a coordinator working from the list sees the wrong pickup time.

- **TRANS-37** A stray "0" shows under every empty address map — Low · Web: Open, Android: Not checked
  - Steps: 1. Open New Trip or Call a Driver. 2. Look under the pickup and dropoff maps before choosing an address.
  - Expected: Nothing shows until an address is picked.
  - Actual: A lone "0" sits under each map. Once an address is chosen it is replaced by the coordinates.

- **TRANS-38** New driver sees "No driver record found" until the page is reloaded — Low · Web: Open, Android: Not checked
  - Steps: 1. Keep Transportation open on web. 2. From another device, change that user's designation to Driver. 3. Read My Trips.
  - Expected: My Trips shows the empty state for a driver with nothing assigned.
  - Actual: The menu switches live to the driver tabs, but My Trips says "No driver record found for your account." The server had already created the record: it was in the coordinator's Drivers list on Android. After a reload the page says "No active trips or ride requests assigned to you."

- **TRANS-39** My Map says location sharing is active when the browser has denied location — Low · Web: Open, Android: Not checked
  - Steps: 1. As a driver with browser location blocked, open My Map while on duty.
  - Expected: The page says location is not being shared, so the driver knows the coordinator can't see them.
  - Actual: The banner says "Location sharing is active" and the badge says Active, while Position stays "Waiting for GPS…" and Last Ping is empty. Only a small note lower down says "User denied Geolocation".

- **TRANS-40** Android Assign Ride offers a vehicle already in use, and the refusal is easy to miss — Medium · Web: Not affected, Android: In progress
  - Steps: 1. Have the only vehicle already assigned. 2. Approve & Assign a ride, pick that vehicle, press Assign.
  - Expected: Only free vehicles are offered, as on web.
  - Actual: "Sunil Gautam - 87654678" is offered. Assign fails with 400 vehicle_already_assigned; only a brief message shows and the sheet stays open, so it looks as if Assign did nothing. The list also offers Vivek Mishra, who web shows as On Trip.

- **TRANS-41** Fleet shows a vehicle as Available when the server says it is already assigned — Medium · Web: Open, Android: In progress
  - Steps: 1. Open Fleet and read the status of "Sunil Gautam 87654678". 2. Try to assign that vehicle to a ride.
  - Expected: A vehicle in use shows as in use, and the Fleet counts agree.
  - Actual: Web Fleet shows AVAILABLE. Android Fleet shows Available 1, In Use 0. Assigning it is refused with vehicle_already_assigned, and web's Assign Driver says "No vehicles are available right now." Fleet and Assign disagree about the same car.

- **TRANS-42** After changing your own designation on web, Transportation keeps the old role until a reload — Medium · Web: Open, Android: Not checked
  - Steps: 1. With Transportation Post off, open Settings → Edit Profile on web. 2. Change Designation to Transportation Coordinator and Submit. 3. Without reloading, open Tools → Transportation.
  - Expected: The new rights apply at once, as they do when the change comes from another device.
  - Actual: The server turns Post on (the phone's rights screen shows it), but web still holds posting_access false 90 s later. The hub shows only My Trips and Ride Requests while the profile card says Transportation coordinator. A reload brings back the full coordinator menu.

- **TRANS-43** Changing a designation silently turns Transportation Post back on — Medium · Web: Open, Android: Open
  - Steps: 1. Turn a user's Transportation Post off. 2. Change their designation to Transport captain or Transportation coordinator, department unchanged. 3. Read their Transportation Post.
  - Expected: Post stays as the admin set it, or the form says this designation grants Post.
  - Actual: Post comes back on with no message, so the user becomes a coordinator and the admin's choice is lost. Seen twice: Transport captain from Android, Transportation coordinator from web.

- **TRANS-44** Permission grid keeps showing Post on after it is turned off — Medium · Web: Open, Android: Not affected
  - Steps: 1. In the grid's Tools view, find a user with Transportation Post on. 2. Untick Posting. 3. Watch the box without reloading.
  - Expected: The box unticks straight away.
  - Actual: The box stays ticked (still on 13 s and 30 s later), although the change saved: the user's phone dropped to passenger within seconds and a reload shows it off. An admin could click again and turn it back on by mistake. Reproduced on 5 and 6 Nov. Ticking on updates normally.

- **TRANS-45** Vehicle lists offer a vehicle that is in Maintenance — Medium · Web: In progress, Android: Not checked
  - Steps: 1. In Fleet, set a vehicle to Maintenance. 2. Open the vehicle list in New Trip or New Route.
  - Expected: Vehicles in Maintenance are not offered.
  - Actual: E2E Test Van, in Maintenance, is offered in both lists, and a shuttle route saved with it and generated runs. New Trip also offers the Sunil Gautam car, which the server refuses as already assigned.

- **TRANS-46** Maintenance card never shows the odometer — Low · Web: In progress, Android: Open
  - Steps: 1. Schedule maintenance with Odometer 12000. 2. Read the card.
  - Expected: The card shows Odometer 12,000 km.
  - Actual: No odometer on the card. The server stored mileage_at_service: 12000, but web reads odometer_reading.

- **TRANS-47** The same maintenance cost shows in different currencies — Low · Web: In progress, Android: In progress
  - Steps: 1. Schedule maintenance with cost 1500 on web. 2. Read the card on web and on Android.
  - Expected: Both show the project's currency.
  - Actual: Web shows ₹1,500, Android shows $1500.00, for the same record. Both look hard-coded.

- **TRANS-48** Fleet says "No vehicles yet." when a search or filter simply has no match — Low · Web: In progress, Android: Not affected
  - Steps: 1. With vehicles in Fleet, search for "zzzz" or filter to In Use.
  - Expected: A no-results message, such as "No vehicles match".
  - Actual: "No vehicles yet.", which reads as if the project has none.

- **TRANS-49** New Trip form is cut off on the left in a narrow Transportation window — Medium · Web: In progress, Android: Not affected
  - Steps: 1. In Windowed view, with the window about 845 px wide, open Transportation. 2. Open Trips → New Trip.
  - Expected: The form fits inside the window and its Close button can be reached.
  - Actual: The drawer stays 960 px wide, so its left side is cut off: the title reads "ew Trip", field labels are clipped and the Close X is out of view. Cancel at the bottom still works.

- **TRANS-50** Driver's allocation card shows a raw number instead of the pickup time — Medium · Web: Not affected, Android: In progress
  - Steps: 1. As a coordinator, create a permanent allocation at 08:00 with this driver. 2. On the driver's phone, open Trips.
  - Expected: The card shows 08:00.
  - Actual: The card shows 1794191400000 where the time belongs. Web shows 08:00 for the same allocation.

- **TRANS-51** Driver can see a coordinator's draft allocation — Low · Web: Not affected, Android: In progress
  - Steps: 1. As a coordinator, save a permanent allocation as a draft with this driver. 2. On the driver's phone, open Trips.
  - Expected: Drafts stay with the coordinator until they are published.
  - Actual: The draft is listed with a Draft badge, so the driver sees unpublished work.

- **TRANS-52** Action Vehicle Reject and Decline return say the reason is optional, then refuse a blank one — Medium · Web: Open, Android: Open
  - Steps: 1. Open Review on a pending vehicle, or Review return. 2. Press Reject or Decline. 3. Leave the box empty and press Done.
  - Expected: Either a blank reason works, or the dialog says a reason is required.
  - Actual: The dialog says "Add an optional reason" and the box says "Reason (optional)", but Done shows "A reason is required." and stays open.

- **TRANS-53** Re-source loses the Source value of a rejected vehicle — Low · Web: Open, Android: Open
  - Steps: 1. Source a vehicle as Rented, send it, reject it. 2. Press Re-source. 3. Press Save & mark sourced.
  - Expected: The form keeps Rented, as it keeps Owner and Contact.
  - Actual: Source is empty and saving shows "Please enter Source", although the record still shows Source: Rented.

- **TRANS-54** Action Vehicle History shows approver changes as "commented" — Low · Web: Open, Android: Not affected
  - Steps: 1. Add an approver and remove one in Track approval. 2. Open History.
  - Expected: "added an approver" and "removed an approver".
  - Actual: Two entries read "iPhone XRR commented", although nobody commented.

- **TRANS-55** Action Vehicle Source form accepts letters as a phone number — Low · Web: Open, Android: Not affected
  - Steps: 1. Choose Source: Rented. 2. Enter "abc" in Contact. 3. Save & mark sourced.
  - Expected: Contact must look like a phone number.
  - Actual: "abc" saved and the vehicle was sourced.

- **TRANS-56** Editing an older Pick Up Now ride is refused as a past schedule — Medium · Web: In progress, Android: Not checked
  - Steps: 1. Create a ride with Pick Up Now. 2. Wait more than 2 minutes, then press Edit. 3. Change only the priority or note and press Save Changes.
  - Expected: The change saves; the pickup time isn't touched.
  - Actual: Edit opens on Schedule Time with the old time, and Save shows "Scheduled pickup time cannot be in the past". The coordinator has to switch to Pick Up Now, which replaces the pickup time with the current time.

- **TRANS-57** Cards say "1 Stops" and "1 Passengers" — Low · Web: In progress, Android: Not affected
  - Steps: 1. Open a ride with one stop and one passenger.
  - Expected: "1 Stop" and "1 Passenger".
  - Actual: "1 Stops" and "1 Passengers".

- **TRANS-58** View Active Cars and passenger My Trips show raw ids instead of names — Medium · Web: In progress, Android: Not affected
  - Steps: 1. Assign a driver and vehicle to a ride. 2. As coordinator, press View Active Cars. 3. As the passenger, open My Trips.
  - Expected: The driver's name and the vehicle's name or plate.
  - Actual: View Active Cars shows "Driver: c45ffa" (the driver is Vivek Mishra). The passenger card shows "Driver #407f04 · Vehicle #c29839".

- **TRANS-59** New Request with Schedule Time and no date is sent as an immediate ride — Medium · Web: In progress, Android: Not affected
  - Steps: 1. Press New Request and fill in pickup and dropoff. 2. Choose Schedule Time. 3. Leave the date empty and press Request Ride.
  - Expected: The form asks for a date and time.
  - Actual: The request saves with no warning as an immediate ride (the card shows the current time).

- **TRANS-60** Every stop added in New Request is saved twice — High · Web: In progress, Android: Not affected
  - Steps: 1. Press New Request and fill in the route. 2. Press Add stops, add one stop and press Save stops. 3. Press Request Ride. 4. Hover the Stops chip on the new card.
  - Expected: One stop.
  - Actual: The card says 2 Stops. The list shows the same address twice, once as PICKUP and once as WAYPOINT, so the driver gets a duplicate stop.

- **TRANS-61** View Active Cars passenger count doesn't match the ride card — Low · Web: In progress, Android: Not affected
  - Steps: 1. Add passengers to an active ride so it has 3. 2. Press View Active Cars.
  - Expected: The same passenger count as the ride card.
  - Actual: The overlay says "2 pax" while the same ride's card says "3 Passengers".

- **TRANS-62** Android user rights screen shows old values after Submit — Medium · Web: Not affected, Android: Open
  - Steps: 1. Open a user's rights on Android. 2. Change Post or the designation and press Submit. 3. Look at the same screen without reopening it.
  - Expected: The screen shows the saved values.
  - Actual: It still shows the old Post and designation until reopened, so another tap can send the wrong value.

- **TRANS-63** Trip Actions offers status steps the server refuses, with raw backend errors — Medium · Web: Open, Android: Not affected
  - Steps: 1. Assign a driver to a trip and press En route to pickup, then Yes. 2. Press At pickup, In transit, At dropoff, Complete or Mark assigned, then Yes.
  - Expected: Only the steps the coordinator can take are enabled, and any refusal is in plain words.
  - Actual: Every step stays enabled. The server refuses them with raw text: "Role 'coordinator' is not authorized for transition en_route_pickup->at_pickup" and "Invalid transition from 'en_route_pickup' to 'completed'". The confirm box also shows internal wording: Transition to "en route pickup"?

- **TRANS-64** A trip link that doesn't exist shows raw shuttle_run_not_found — Low · Web: In progress, Android: Not affected
  - Steps: 1. Open /film-tools/transportation-hub/trips/000000000000000000000000.
  - Expected: A plain "Trip not found" message with a way back to Trips.
  - Actual: The page shows only the text shuttle_run_not_found, with no Back to trips button.

- **TRANS-65** Escape doesn't close New Request in windowed view until you click inside it — Low · Web: Open, Android: Not affected
  - Steps: 1. Turn on Windowed view and open Transportation. 2. Press New Request. 3. Press Escape straight away.
  - Expected: The form closes, as it does in Classic view.
  - Actual: Nothing happens. It closes only after clicking inside the form first. Classic view closes on the first Escape.

- **TRANS-66** Action Vehicle Notes are saved but never shown — Medium · Web: Open, Android: Not affected
  - Steps: 1. Request a vehicle and type in Notes ("Anything the transport team should know"). 2. Save, then open the request card.
  - Expected: The note appears in the details for the transport team.
  - Actual: The note doesn't appear on the card or in the details panel. It is only visible by opening Edit, which sourcers don't use.

- **TRANS-67** New Trip refuses phone numbers with a country code and shows a raw database error — High · Web: In progress, Android: Not affected
  - Steps: 1. Press New Trip and fill in a guest passenger. 2. Type +91 90000 00012 (or letters) in Contact Number. 3. Press Create Trip.
  - Expected: A normal phone number saves. Letters get a plain message next to the field.
  - Actual: The trip isn't created. The form shows "TripRequestV2 validation failed: passengers.1.contact_number: Cast to Number failed for value...". Only digits with no + or spaces work.

- **TRANS-68** Request a Ride keeps light fields in the dark theme — Low · Web: Open, Android: Not checked
  - Steps: 1. Switch the hub to dark. 2. Open New Request.
  - Expected: All fields follow the dark theme.
  - Actual: The pickup and dropoff boxes, Schedule Time and Same as yesterday stay bright white, and the priority labels are low-contrast grey.

- **TRANS-69** Dashboard Pending chip says "No trips yet" when trips exist — Low · Web: In progress, Android: Not affected
  - Steps: 1. With no pending trips, press the Pending chip.
  - Expected: A message that no trips are pending.
  - Actual: "No trips yet", although the project has 12 trips.

- **TRANS-70** A cancelled trip leaves its vehicle marked Allocated — Medium · Web: Open, Android: Open
  - Steps: 1. Set a vehicle to Available. 2. Assign it to a trip (Fleet shows Assigned). 3. Cancel the trip and open Fleet.
  - Expected: The vehicle goes back to Available.
  - Actual: Fleet shows Allocated, although no trip or allocation uses it. Reproduced twice. The old Vehicle List shows the same vehicle as Available For Trips, and Assign still offers it, so only the Fleet status is wrong.

- **TRANS-71** Live Map Trips pill counts rejected trips as active — Low · Web: In progress, Android: Not affected
  - Steps: 1. Have rejected trips in the project. 2. Open Live Tracking and read the Trips pill. 3. Compare with Active Trips on the Dashboard.
  - Expected: The same active count as the Dashboard.
  - Actual: The pill shows 5 while the Dashboard shows 1 active trip.

- **TRANS-72** Dashboard Recent Trips shows dashes for a permanent allocation's route — Low · Web: In progress, Android: Not affected
  - Steps: 1. Publish a permanent allocation with a pickup and dropoff. 2. Open the Dashboard.
  - Expected: The pickup and dropoff addresses, as on the Trips page.
  - Actual: The row shows "—" for both addresses. Trips shows them correctly for the same trip (#C44F56).

- **TRANS-73** Allocation Edit shows the driver as a raw id after Swap Driver — Low · Web: In progress, Android: Not checked
  - Steps: 1. Give a driver a trip for today. 2. On an active allocation, press Swap Driver and pick that driver. 3. Press Edit on the allocation.
  - Expected: The Driver field shows the driver's name.
  - Actual: The Driver field shows 6af2c1738ca825faf0827a9c. Edit's driver list leaves out drivers who already have a trip, while Swap Driver offers them, so Edit can't name the current driver. Saving still keeps the right driver.

- **TRANS-74** A driver can cancel a coordinator's trip from the trip details — Medium · Web: Open, Android: Open
  - Steps: 1. Assign a trip to a driver who has no Post. 2. As that driver, open View Detail on the trip. 3. Press Cancel, then Yes.
  - Expected: Only the coordinator can cancel a trip. The driver sees just their own steps.
  - Actual: The driver sees every status button, including Cancel and Mark assigned. Cancel works: "Status updated to cancelled".

- **TRANS-75** Android Assign Ride shows no drivers when the on-duty driver comes from a designation — Medium · Web: Not affected, Android: To verify
  - Steps: 1. Have one on-duty driver who is a driver by designation (Vivek Mishra, no phone or licence on file). 2. On Android, press Approve & Assign on a ride and open Select driver.
  - Expected: The on-duty driver is listed, as on web.
  - Actual: The list opens empty. The server returned Vivek with is_on_duty true and is_available true, and web Assign lists him.

- **TRANS-76** Android Create Trip drops the title and the pickup date and time — High · Web: Not affected, Android: Open
  - Steps: 1. Press Create Trip and type a title. 2. Pick a passenger, today's date, a pickup time (say 18:27), pickup and dropoff. 3. Press Create Trip and open the trip.
  - Expected: The trip has the title and the scheduled pickup.
  - Actual: The title is empty (the trip shows only Trip #F082B749) and Scheduled Pickup is blank. Pickup Time shows when it was created (1:29 PM). The web shows the same trip with no title and "No pickup time". A trip created with a driver also stays Pending.

- **TRANS-77** Android Trips cards show no time and are titled with the creator — Low · Web: Not affected, Android: Open
  - Steps: 1. Open Trips on Android. 2. Look at a trip scheduled for 18:00 with passenger Guest Three.
  - Expected: The card shows the trip title or passenger, and the pickup time.
  - Actual: The card is titled "iPhone XRR" (who created it) and its clock icon has no time next to it, on every card. The trip's details show 6:00 PM correctly.

- **TRANS-78** Android Assign Trip shows "Trip assigned" when the server refused — High · Web: Not affected, Android: Open
  - Steps: 1. Give a driver another trip. 2. On Android, open a pending trip, press Assign and pick that driver and a vehicle. 3. Press Assign.
  - Expected: A clear message that the driver is busy, and the list shouldn't offer a busy driver.
  - Actual: A green "Trip assigned" toast appears, but the server answered driver_already_engaged and the trip stays unassigned. The list also offers the busy driver.

- **TRANS-79** Android Add Vehicle closes and loses the form when the server refuses it — Low · Web: Not affected, Android: Open
  - Steps: 1. Add a vehicle with a plate that already exists (E2E-1107). 2. Press Add.
  - Expected: The form stays open with the message, so the plate can be corrected.
  - Actual: The toast "Vehicle number already exists." shows, but the sheet closes and everything typed is lost.

- **TRANS-80** Android New Request quietly moves a past morning time to the evening — Low · Web: Not affected, Android: Open
  - Steps: 1. Choose Schedule Time and today's date. 2. Pick 9:34 AM, which has already passed, and press OK.
  - Expected: A message that the time has passed, or the chosen time is kept and refused on submit.
  - Actual: The field shows 9:34 pm, twelve hours later, with no message. Choosing 9:34 AM tomorrow keeps AM.

- **TRANS-81** Android approver list offers people without Transportation Post — Medium · Web: Not affected, Android: Open
  - Steps: 1. Source a vehicle on Android. 2. Press Send for approval.
  - Expected: Only people who can approve (Transportation Post) are listed, as on web.
  - Actual: Every project member is listed. Sending to android peach (Screen writer, no Post) was accepted by the server.

- **TRANS-82** Android Edit Shuttle Route says saved but drops time changes — Medium · Web: Not affected, Android: Open
  - Steps: 1. Edit a route and change the End time from 22:00 to 21:00. 2. Press Save changes.
  - Expected: The route ends at 21:00.
  - Actual: "Updated" comes back but the route still runs 06:00–22:00 on Android and web. An invalid 05:00 end was dropped the same way, with no message.

- **TRANS-83** Android maintenance "Title" is saved as the notes — Low · Web: Not affected, Android: Open
  - Steps: 1. Pick a vehicle and type a Title ([e2e-th] Android oil). 2. Leave Description empty and press Schedule.
  - Expected: The record is titled with what was typed.
  - Actual: The card is titled with the type (Oil Change). On web the typed title appears under NOTES.

- **TRANS-84** Android passenger's new ride doesn't appear until Transportation is reopened — Low · Web: Not affected, Android: Open
  - Steps: 1. As a passenger, request a ride. 2. Look at the Rides list, then tap the Rides tab again.
  - Expected: The new ride appears at the top straight away.
  - Actual: The server confirms it, but the list doesn't show it until you leave Transportation and come back.
