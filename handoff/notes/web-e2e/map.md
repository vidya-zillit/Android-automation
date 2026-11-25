# Web E2E — Map

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: deep — Cities, zones, types, locations, Places, directions, rights, two users

## Web test cases (220)

### Map — entry, shell and toolbar

- **the route opens the map and reads cities, types and the open city @smoke** — passed (map.spec.js:283)
  - Where: Tools → Map · account: iPhone XRR
  - Steps: Open Tools → Map → Wait for the map to load with the “How To Use Map” button in its toolbar → Check that the list of cities and the list of location types have loaded → If a city opened by itself, check that its locations and studio zones have loaded → If the project has no city, check that the floating “Add City” button shows instead → In the windowed view, check that the window title reads “Map”
  - Expected: The map opens with its toolbar and loads the cities, the location types and, for the city it opens, that city’s locations and studio zones. In the windowed view the window is titled “Map”.

- **the Tools tile "Map" opens the map @smoke** — passed (map.spec.js:302)
  - Where: Tools · account: iPhone XRR
  - Steps: Open Tools → Find the “Map” tile in the Tools grid → Click the “Map” tile → Check that the map opens with the “How To Use Map” button in its toolbar
  - Expected: Clicking the “Map” tile on the Tools page opens the map.

- **"Back to Film Tools" closes the window (Windowed) or lands on the Tools grid (Classic) @smoke** — passed (map.spec.js:311)
  - Where: Tools → Map · account: iPhone XRR
  - Steps: Open Tools → Map → Note whether the app is in the windowed view (tool windows) or the Classic view (full page) → Click the back arrow “Back to Film Tools” at the left of the map toolbar → In the windowed view, check that the Map window closes → In the Classic view, check that the Tools grid shows
  - Expected: “Back to Film Tools” closes the Map window in the windowed view, or returns to the Tools grid in the Classic view.

- **a city opens on its own: the only one, else the last visited, else the first @smoke** — passed (map.spec.js:321)
  - Where: Tools → Map · account: iPhone XRR
  - Steps: Note which city the map showed the last time it was opened in this browser (if any) → Open Tools → Map → Read the name of the city that opens by itself in the toolbar → Check that it is the only city when the project has one city; otherwise the last visited city if it still exists; otherwise the first city in the Cities list → Check that the app now remembers this city as the last visited one
  - Expected: The map opens a city on its own: the only city, else the last visited one, else the first in the list.

- **the toolbar is complete and the header counts map locations plus Location-tool pins @smoke** — passed (map.spec.js:332)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Check that the toolbar shows Fit All, Search, Filter, the pin button, List View, Studio Zone, LOC Types, Full View, the theme toggle and “How To Use Map” → Count the open city’s map locations → Add the Location-tool pins (Shortlisted / Final locations) that fall inside this city → Check that the count next to the city name reads “N locations” with that total → Check that the List View button shows the same number (when it is above 0)
  - Expected: Every toolbar button is present, and the header count and the List View number equal the city’s map locations plus the Location-tool pins inside the city.

- **the open city's name stays readable in the toolbar at the default 1280×720 window @smoke** — suspected · known bug the toolbar pills squeeze the city name (h2 with min-w-0 + truncate) to nothing at 1280×720 — run 1 screenshots show "6 loca" / "C." where the name should be,the toolbar pills squeeze the city name (h2 with min-w-0 + truncate) to nothing at 1280×720 — run 1 screenshots show "6 loca" / "C." where the name should be,the toolbar pills squeeze the city name (h2 with min-w-0 + truncate) to nothing at 1280×720 — run 1 screenshots show "6 loca" / "C." where the name should be (map.spec.js:357)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Set the browser window to 1280 × 720 → Open Tools → Map → Look at the open city’s name at the left of the toolbar → Check that the name has room to be read (wider than about 40 pixels)
  - Expected: The city name stays readable in the toolbar at 1280 × 720. Known bug: the toolbar buttons squeeze the name to almost nothing, so only fragments such as “6 loca” or “C.” show.
  - Web error: Error: the city name has room in the toolbar

- **the guide collapses, dismisses for the session and comes back from "How To Use Map" @smoke** — passed (map.spec.js:358)
  - Where: Tools → Map, in a new browser tab so the map guide shows · account: iPhone XRR
  - Steps: Open Tools → Map and find the guide “How to Use the Map Module” → Check that it is expanded and lists three steps: add the city, create a Studio Zone (if required), add locations by placing pins → Click the guide’s title and check that the guide collapses → Click the title again and check that the steps show again → Click the guide’s close (X) button and check that the guide disappears → Click “How To Use Map” in the toolbar and check that the guide comes back expanded → Close the guide again with its X → Reload the map and check that the guide stays hidden
  - Expected: The guide collapses and expands from its title, closes with its X, comes back from “How To Use Map”, and stays closed after a reload in the same tab.

- **the theme toggle flips the map, persists in zillit-app-theme and is restored @smoke** — passed (map.spec.js:388)
  - Where: Tools → Map · account: iPhone XRR
  - Steps: Open Tools → Map and note whether it is light or dark → Click the theme toggle (“Switch to dark mode” or “Switch to light mode”) → Check that the map switches to the other theme and the toggle’s label flips → Reload the map → Check that the map still shows the new theme → Click the toggle again → Check that the map is back in its original theme
  - Expected: The toggle switches the map between light and dark, the choice survives a reload, and toggling again restores the original theme.

- **Full View fullscreens the map root and keeps its drawers inside it @smoke** — passed (map.spec.js:411)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Click “Enter Full View” in the toolbar → Check that the map fills the screen and the button now reads “Exit Full View” → Click LOC Types → Check that the Location Types drawer opens inside the full-screen map → Close the drawer → Click “Exit Full View” → Check that the map leaves full screen and the button reads “Enter Full View” again
  - Expected: Full View puts the map in full screen with its drawers still shown inside it, and Exit Full View brings it back.

- **the floating Cities button counts the cities; the chip names the open city @smoke** — passed (map.spec.js:429)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Check that the number on the floating “Cities” button equals the number of cities (99+ above 99) → Check that the city chip next to it names the open city → Click Search in the toolbar → Check that the Cities button and the city chip hide → Click Search again → Check that the Cities button comes back
  - Expected: The floating Cities button counts the cities and the chip names the open city; both hide while Search is open.

- **pin mode toggles from the toolbar and the banner, and says where a pin may go @smoke** — passed (map.spec.js:444)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Click “Pin New Location” in the toolbar → Check that the button turns into “Cancel” → Check the banner: “Click inside “<zone>” to place a pin (restricted to zone)” when a studio zone is drawn, else “Click on the map to place a pin” → Click the close button at the end of the banner → Check that the banner hides and the button is “Pin New Location” again → Click the pin button and check that the banner shows → Click the pin button again and check that the banner hides
  - Expected: Pin mode turns on and off from the toolbar and from the banner, and the banner says where a pin may go. Without the Map posting right, the pin button instead asks to request rights (the test answers No) and pin mode stays off.

### Map — drawers and read views on sandbox data

- **the Cities drawer lists every city, highlights the open one and closes @smoke** — passed (map.spec.js:473)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Click the floating “Cities” button → Check that the Cities drawer says “Switch between cities or add a new one to the map.” → Check that it shows “N Cities” with the number of cities, and one row per city → Check that the open city’s row is highlighted with a ring → Check that “Add Another City” shows only when the account may post on Map → Close the drawer
  - Expected: The Cities drawer lists every city with the right count, highlights the open one, offers “Add Another City” to posters only, and closes.

- **"Add Another City" opens an empty Add City form; Cancel sends nothing @smoke** — passed (map.spec.js:489)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Click the floating “Cities” button → Click “Add Another City” → Check that the Add City dialog shows an empty “Search for a city...” field → Check that the two coordinate fields (“Auto-filled”) cannot be typed in → Check that the “Add City” button is disabled → Click Cancel → Check that the dialog closes and no city was created
  - Expected: “Add Another City” opens an empty Add City dialog with read-only coordinates and a disabled Add City button; Cancel closes it without creating anything.

- **search opens focused, hides the Cities button and reports no match @smoke** — passed (map.spec.js:508)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Click Search in the toolbar → Check that the search box “Search locations by name, address, or type...” opens with the cursor in it → Check that the floating Cities button hides → Type “zz-e2e-no-such-location” → Check that “No locations found” shows → Click Search again → Check that the search box closes and the Cities button comes back
  - Expected: Search opens ready to type, hides the Cities button, reports “No locations found” for a name that does not exist, and closes again.

- **the type filter offers one chip per type present (untyped = Unknown) @smoke** — passed (map.spec.js:523)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → List the different location types among the city’s locations, counting untyped ones as “Unknown” and Location-tool pins as “Final” or “Shortlisted” → Click Filter in the toolbar → Check that the “Filter by Type” panel shows one chip per type in that list → If the city has no locations, check that it says “No types available” → Click Search → Check that the filter panel closes
  - Expected: The type filter offers exactly one chip per type present in the city (untyped locations as “Unknown”), and opening Search closes it.

- **the List View drawer shows one card per map location of the city @smoke** — passed (map.spec.js:543)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Click List View → Wait for “Loading locations...” to go → Check that the drawer shows one card per map location of the city, or “No locations yet” when there are none → Check that the drawer’s count reads “N Locations” with the same number → Close the drawer
  - Expected: The List View drawer shows one card per map location of the open city and the matching “N Locations” count.

- **the Studio Zones drawer re-reads the zones and lists them @smoke** — passed (map.spec.js:556)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Click the Studio Zone button in the toolbar → Check that the zones are loaded again when the drawer opens → Check that the drawer shows “N Zones” with the city’s number of zones → If there are none, check for “No studio zones yet” and an “Add Studio Zone” button → Otherwise check that there is one row per zone, and exactly one row marked “Active” when the toolbar shows “Zone: …” → Close the drawer
  - Expected: The Studio Zones drawer reloads the zones and lists them with the right count (or its empty state), with the drawn zone marked Active.

- **the LOC Types drawer counts the types and filters to nothing @smoke** — passed (map.spec.js:576)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map → Click LOC Types → Check that the Location Types drawer shows “N Types” with the project’s number of location types → Type “zz-e2e-none” in “Filter by type or subtype...” → Check that it says “No types match “zz-e2e-none”” → If the project has no types, check for “No location types yet” instead → Close the drawer
  - Expected: The LOC Types drawer counts the location types, and a filter that matches nothing says so.

- **the location form validates name, address and a custom "Other" type without sending @smoke** — passed (map.spec.js:592)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map, click List View, then “New Location” → Press “Save Location” with the form empty → Check that it says “Please fill in required fields” → Type “[e2e-map] validation …” as the location name and press “Save Location” → Check that it says “Please enter an address or search for a location” → If the project has a location type “Other”: pick it, check that “Enter custom type name” appears, type an address and press “Save Location” → Check that it says “Please specify the custom location type” → Press Cancel → Check that the form closes and nothing was saved
  - Expected: The New Location form refuses to save without a name, without an address, and with the “Other” type but no custom type name, and nothing is sent.

- **photos preview, remove and cap at ten without saving @smoke** — passed (map.spec.js:626)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Steps: Open Tools → Map, click List View, then “New Location” → Add two small PNG images as photos → Check that two photo previews marked “New” show → Click Remove on one of them and check that one is left → Add eleven more images at once → Check that it says “Only 9 of 11 files added (limit: 10)” and ten previews show → Add one more image → Check that it says “Maximum 10 media allowed” and there are still ten previews → Press Cancel and check that nothing was saved
  - Expected: Photos preview before saving, can be removed, and are capped at ten with a message; cancelling saves nothing.

- **the toolbar count matches what the List View drawer lists @smoke** — suspected · known bug the toolbar List View badge and the header count include Location-tool pins that the List View drawer never lists,the toolbar List View badge and the header count include Location-tool pins that the List View drawer never lists,the toolbar List View badge and the header count include Location-tool pins that the List View drawer never lists (map.spec.js:660)
  - Where: Tools → Map, on a city that has Location-tool pins (Shortlisted / Final locations) · account: iPhone XRR
  - Steps: Open Tools → Map → Click List View → Count the location cards in the drawer → Close the drawer → Read the location count next to the city name in the toolbar → Check that the toolbar count equals the number of cards
  - Expected: The toolbar count equals the number of locations the List View lists. Known bug: the toolbar count (and the List View number) also include Location-tool pins that the drawer never lists.
  - Web error: Error: expect(received).toBe(expected) // Object.is equality

- **Location-tool pins are read and filed under the open city, with Final / Shortlisted chips @smoke** — passed (map.spec.js:670)
  - Where: Tools → Map, on a city with Shortlisted or Final locations from the Location tool · account: iPhone XRR
  - Steps: Open Tools → Map → Check that the map loads the Location-tool pins → Find the Location-tool pins that fall inside the open city → Click Filter in the toolbar → Check that a “Final” chip shows if any of them is Final, and a “Shortlisted” chip if any is Shortlisted
  - Expected: The map reads the Location-tool pins and offers “Final” / “Shortlisted” filter chips for those inside the open city.

- **a Location-tool pin has no Edit and "View" opens the Location tool @smoke** — passed (map.spec.js:687)
  - Where: Tools → Map, on a city with a named Shortlisted or Final location from the Location tool · account: iPhone XRR
  - Steps: Open Tools → Map → Click Search and type the name of a Location-tool pin in this city → Click the result → Check that the card shows the pin’s status, “Final” or “Shortlisted” → Check that the card has no Edit button → Click View on the card → Check that the Location tool opens (a new window in the windowed view, or the Location page in the Classic view)
  - Expected: A Location-tool pin’s card shows its status, offers no Edit, and its View button opens the Location tool.

- **a Location-tool pin is not listed in the List View drawer @smoke** — passed (map.spec.js:705)
  - Where: Tools → Map, on a city with a named Shortlisted or Final location from the Location tool · account: iPhone XRR
  - Steps: Open Tools → Map → Pick the name of a Location-tool pin that falls inside the open city → Click List View → Check that no card in the drawer carries that name
  - Expected: Location-tool pins show on the map but are not listed in the List View drawer.

- **without the location_tool view right no Location-tool pin is shown @smoke** — passed (map.spec.js:718)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test makes the rights reply say the account cannot view the Location tool.
  - Steps: Open Tools → Map as a user without the Location tool view right → Count the open city’s own map locations → Read the location count next to the city name → Check that it equals the map locations alone, with no Location-tool pins added
  - Expected: Without the Location tool view right, no Location-tool pin is counted or shown for the city.

- **a pin filed under another city is not searchable in this one @smoke** — skipped (map.spec.js:735)
  - Where: Tools → Map, in a project whose Location-tool pins are not all inside the open city · account: iPhone XRR
  - Steps: Open Tools → Map → Pick the name of a Location-tool pin that lies outside the open city → Click Search in the toolbar → Type that pin’s name → Check that it says “No locations found”
  - Expected: A Location-tool pin filed under another city cannot be found by searching in this city.

### Map — stubbed states (the stub answers; nothing reaches the backend) › geolocation denied

- **without geolocation, directions ask for a pickup and "use my location" fails politely @smoke** — passed (map.spec.js:1052)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub pin” (address “[e2e-map] stub address”) to the open city’s location list; nothing is saved. The browser is not allowed to share its location.
  - Steps: Open Tools → Map with location access blocked in the browser → Search for “[e2e-map] stub pin” and open its card → Click Directions → Check that a toast says “Enable location access or enter a pickup location manually” → Check that the Get Directions panel shows an empty “Pickup location” and the location’s address as “Drop-off / Destination” → Click “Use my current location” → Check that a toast says “Could not get your current location” → Close the directions panel with its X
  - Expected: Without location access, Directions asks for a pickup, keeps the destination, and “Use my current location” fails with a polite message.

### Map — stubbed states (the stub answers; nothing reaches the backend)

- **zero cities: the bare toolbar, the floating Add City and the empty drawer @smoke** — passed (map.spec.js:755)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: The test makes the server answer that the project has no cities.
  - Steps: Open Tools → Map in a project with no cities → Check that no city name shows and the floating “Add City” button shows → Check that the toolbar has no Fit All, Search, Filter, pin button, List View, Studio Zone or LOC Types → Check that Full View and the theme toggle are still there → Click “Add City” → Check that the Cities drawer says “No cities added yet” and offers “Add Another City” → Check that no location card shows on the map
  - Expected: With no cities the toolbar keeps only Full View and the theme toggle, a floating “Add City” opens the Cities drawer with “No cities added yet” and “Add Another City”. Whether the Current Location suggestion shows is only recorded.

- **Fit All with nothing to show says so @smoke** — passed (map.spec.js:776)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test makes the server answer that the open city has no locations and no Location-tool pins.
  - Steps: Open Tools → Map on a city with no locations → Check that the count next to the city name reads “0 locations” → Click Fit All → Check that a message says “No locations to zoom to”
  - Expected: Fit All on a city with nothing to show says “No locations to zoom to”.

- **an empty city shows the List View empty state @smoke** — passed (map.spec.js:794)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test makes the server answer that the open city has no locations.
  - Steps: Open Tools → Map on a city with no locations → Click List View → Check that the drawer says “No locations yet” → Check that it offers an “Add Location” button
  - Expected: An empty city’s List View shows “No locations yet” with an “Add Location” button.

- **view_access off hides the tile but the route still opens the map** — bug · known bug the map never checks view_access (no useToolDisabledRedirect): a deep link opens it for a user the Tools grid hides it from — confirmed run 1,the map never checks view_access (no useToolDisabledRedirect): a deep link opens it for a user the Tools grid hides it from — confirmed run 1 (map.spec.js:795)
  - Where: Tools, then the map opened from a saved direct link · account: iPhone XRR
  - Setup: The test makes the rights reply say the account may not view the Map tool.
  - Steps: Open Tools as a user without the Map view right → Check that there is no “Map” tile → Open the map through a saved direct link to it → Check that the map does not open
  - Expected: A user without the Map view right sees no Map tile and cannot reach the map by a direct link either. Known bug: the direct link still opens the map.
  - Web error: Error: a user without view_access should not reach the map

- **a project config without the Maps key leaves "Loading Google Maps..." up @smoke** — passed (map.spec.js:814)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: The test removes the Google Maps key from the project settings the server sends.
  - Steps: Open Tools → Map in a project whose settings have no Google Maps key → Check that “Loading Google Maps...” shows → Wait 10 seconds → Check that “Loading Google Maps...” is still shown and the map never loads
  - Expected: Without a Google Maps key the map stays on “Loading Google Maps...”; no timeout or error message appears (recorded).

- **a refused city list toasts the server message and shows the empty state @smoke** — passed (map.spec.js:832)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: The test makes the server refuse the city list with an error message.
  - Steps: Open Tools → Map → Check that a toast shows the server’s error message → Check that the map shows the no-city state with the floating “Add City” button
  - Expected: A refused city list shows the server’s message in a toast and the empty no-city state.

- **an HTTP 500 city list without an envelope is reported** — bug · known bug isApiFailure is status === 0 only: an HTTP error without an envelope reads as an empty city list, with no error — confirmed run 1,isApiFailure is status === 0 only: an HTTP error without an envelope reads as an empty city list, with no error — confirmed run 1 (map.spec.js:840)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: The test makes the city list fail with a server error that has no message.
  - Steps: Open Tools → Map → Check that the map shows the no-city state with the floating “Add City” button → Check that an error toast reports the failed city list
  - Expected: A city list that fails with a server error is reported with an error toast. Known bug: no error is shown; the map acts as if the project simply had no cities.
  - Web error: Error: an error toast for the failed city list

- **a refused location list toasts the server message @smoke** — passed (map.spec.js:854)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test makes the server refuse the open city’s location list with an error message.
  - Steps: Open Tools → Map → Wait for the city to open and its locations to load → Check that a toast shows the server’s error message
  - Expected: A refused location list shows the server’s message in a toast.

- **a refused studio-zone list toasts the server message @smoke** — passed (map.spec.js:854)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test makes the server refuse the open city’s studio-zone list with an error message.
  - Steps: Open Tools → Map → Wait for the city to open and its studio zones to load → Check that a toast shows the server’s error message
  - Expected: A refused studio-zone list shows the server’s message in a toast.

- **a refused location-type list toasts the server message @smoke** — passed (map.spec.js:854)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: The test makes the server refuse the list of location types with an error message.
  - Steps: Open Tools → Map → Wait for the map to load the location types → Check that a toast shows the server’s error message
  - Expected: A refused location-type list shows the server’s message in a toast.

- **a refused zone save toasts the server message and keeps the form open** — passed (map.spec.js:863)
  - Where: Tools → Map, on the city that opens by itself → Studio Zone · account: iPhone XRR
  - Setup: The test makes the zone save fail with an error message, so nothing is saved.
  - Steps: Open Tools → Map → Click the Studio Zone button, then “Add Zone” → Type “[e2e-map] stub zone …” as the Zone Name → Press “Create Zone” → Check that a toast shows the server’s error message → Check that the New Studio Zone form stays open → Press Cancel
  - Expected: A refused zone save shows the server’s message and keeps the form open.

- **slow reads show the List View and Studio Zones loaders @smoke** — passed (map.spec.js:882)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test slows the location list and then the studio-zone list down by 5 seconds each.
  - Steps: Open Tools → Map → Click List View → Check that “Loading locations...” shows, then goes → Close the drawer → Click the Studio Zone button → Check that a loading spinner shows in the Studio Zones drawer, then goes
  - Expected: Slow reads show “Loading locations...” in List View and a spinner in Studio Zones until the data arrives.

- **with no city and a silent geolocation the map shows "Locating you..." then renders @smoke** — passed (map.spec.js:898)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: The test makes the server answer that the project has no cities, and makes the browser’s location request never answer.
  - Steps: Open Tools → Map in a project with no cities → Check that “Locating you...” shows → Wait up to 15 seconds → Check that “Locating you...” goes and the map itself shows
  - Expected: With no city and no answer from the browser’s location, the map shows “Locating you...” and then renders anyway.

- **a location opened from the search shows its card: address, coordinates and actions @smoke** — passed (map.spec.js:917)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub pin” (address “[e2e-map] stub address”) to the open city’s location list; nothing is saved.
  - Steps: Open Tools → Map → Click Search and type “[e2e-map] stub pin” → Click the result → Check that the search box closes and the location’s card opens on the map → Check that the card shows the address and the coordinates as “latitude, longitude” with six decimals → Check that it has Edit, Share, View and Directions buttons → Click the card’s close (X) button → Check that the card closes
  - Expected: A location picked from the search opens its card with address, coordinates and the Edit, Share, View and Directions actions, and the card closes.

- **the share menu offers Forward In App and Share (Remote only for an own top-level project) @smoke** — passed (map.spec.js:935)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub pin” (address “[e2e-map] stub address”) to the open city’s location list; nothing is saved.
  - Steps: Open Tools → Map → Search for “[e2e-map] stub pin” and open its card → Click Share on the card → Check that the menu has one “Forward In App” item and one “Share” item → Click Share again → Check that the menu closes
  - Expected: The share menu offers “Forward In App” and “Share” (email). Whether “Forward to Remote Projects” is offered is only recorded, not checked.

- **Forward In App opens the forward picker and closes without sending @smoke** — passed (map.spec.js:952)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub pin” (address “[e2e-map] stub address”) to the open city’s location list; nothing is saved.
  - Steps: Open Tools → Map → Search for “[e2e-map] stub pin” and open its card → Click Share, then “Forward In App” → Check that the “Select Tab To Forward” dialog opens → Close it with its X → Check that the dialog closes and nothing is sent
  - Expected: “Forward In App” opens the “Select Tab To Forward” picker, which closes without sending anything.

- **Forward In App to the sandbox group sends the pin (real send)** — skipped (map.spec.js:966)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub pin” (address “[e2e-map] stub address”) to the open city’s location list; nothing is saved.
  - Steps: Open Tools → Map → Search for “[e2e-map] stub pin” and open its card → Click Share, then “Forward In App” → In “Select Tab To Forward”, choose Group Chat → Click the sandbox group “test 001” → Press Send (or Forward) if the app asks → Check that a message confirms the forward
  - Expected: The pin is forwarded to the sandbox group chat and the app confirms the forward.

- **Forward to Remote Projects opens the project picker; backing out clears linkApidetails @smoke** — passed (map.spec.js:986)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub pin” (address “[e2e-map] stub address”) to the open city’s location list; nothing is saved.
  - Steps: Open Tools → Map → Search for “[e2e-map] stub pin” and open its card → Click Share, then “Forward to Remote Projects” → Check that the “Forward to Remote Projects” picker opens → Close it with its X → Check that the picker closes and the app forgets the remote-project link it had stored
  - Expected: “Forward to Remote Projects” opens its project picker, and backing out closes it and clears the stored remote-project link.

- **Share (email) opens Compose with the pin text, then Cancel** — failed (map.spec.js:1006)
  - Web error: Test timeout of 90000ms exceeded.

- **a typed address without a picked place is not submitted** — bug · known bug buildAndSubmit checks only that the address text is non-empty: a typed, unpicked address POSTs location {lat: null, long: null} — confirmed run 3 (every run so far),buildAndSubmit checks only that the address text is non-empty: a typed, unpicked address POSTs location {lat: null, long: null} — confirmed run 3 (every run so far) (map.spec.js:1043)
  - Where: Tools → Map, on the city that opens by itself → List View → New Location · account: iPhone XRR
  - Setup: The test answers the location save itself, so nothing reaches the server.
  - Steps: Open Tools → Map, click List View, then “New Location” → Type “[e2e-map] typed address …” as the location name → Type “[e2e-map] typed, not picked” in the address field without picking a suggestion → Click the name field → Press “Save Location” → Wait 3 seconds → Check that no save was sent
  - Expected: An address that is typed but not picked from the suggestions (so it has no coordinates) is not saved. Known bug: the form sends the location anyway, with empty coordinates (seen in every run).
  - Web error: Error: a location without coordinates must not be submitted

### Map — seeded: cities (serial)

- **the "Current Location" suggestion adds the e2e city and opens it** — passed (map.spec.js:1094)
  - Where: Tools → Map → Cities, with the browser’s location set near Alice Springs, Australia · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs (e2e cities and “[e2e-map]” types, locations and zones).
  - Steps: Set the browser’s location to a point near Alice Springs → Open Tools → Map and click the floating “Cities” button → Wait for the “Current Location” suggestion card and note the city name it shows → Click “Add” on the card → In the Add City dialog press “Add City” → Check that a toast says “City added successfully” → Check that the city is saved at the browser’s exact location (same point as its centre, radius 0) → Check that the new city opens in the toolbar and the Cities drawer closes
  - Expected: The suggested city is created at the browser’s location, “City added successfully” shows, and the map switches to the new city.

- **a second e2e city from another geolocation** — passed (map.spec.js:1119)
  - Where: Tools → Map → Cities, with the browser’s location set near Tennant Creek, Australia · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06.
  - Steps: Set the browser’s location to a point near Tennant Creek → Open Tools → Map and click the floating “Cities” button → Wait for the “Current Location” suggestion card and click “Add” → In the Add City dialog press “Add City” → Check that the city is created → Check that its name differs from the first e2e city
  - Expected: A second e2e city is created from the new browser location, with a different name from the first.

- **the drawer lists both e2e cities and rings the open one** — passed (map.spec.js:1128)
  - Where: Tools → Map, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06, and the second e2e city (from a browser location near Tennant Creek, Australia), added by MAP-C06b.
  - Steps: Open Tools → Map with the first e2e city open → Click the floating “Cities” button → Check that the drawer shows “N Cities” with the number of cities in the project → Check that the first e2e city’s row is highlighted with a ring → Check that the second e2e city’s row is not highlighted → Check that “Add Another City” shows → Close the drawer
  - Expected: The Cities drawer lists both e2e cities with the right count, rings only the open one, and offers “Add Another City”.

- **the city search filters the rows and says when nothing matches** — passed (map.spec.js:1143)
  - Where: Tools → Map → Cities, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06.
  - Steps: Open Tools → Map and click the floating “Cities” button → Type the first five letters of the e2e city’s name in “Search cities...” → Check that the e2e city’s row shows → Type “zz-e2e-no-such-city” → Check that it says “No cities match your search” → Clear the search box → Check that the e2e city’s row shows again
  - Expected: The city search filters the rows and says “No cities match your search” when nothing matches.

- **selecting another city re-reads it, remembers it and resets the open panels** — passed (map.spec.js:1156)
  - Where: Tools → Map, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06, and the second e2e city (from a browser location near Tennant Creek, Australia), added by MAP-C06b.
  - Steps: Open Tools → Map with the first e2e city open → Click Filter so the “Filter by Type” panel is open → Click the floating “Cities” button → Click the second e2e city → Check that the second city opens and its locations and studio zones are loaded → Check that the filter panel has closed → Check that the app remembers the second city as the last visited one
  - Expected: Switching city loads the new city’s locations and zones, closes the open filter panel and remembers the new city.

- **switching city also clears an active type filter** — passed (map.spec.js:1173)
  - Where: Tools → Map, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06, and the second e2e city (from a browser location near Tennant Creek, Australia), added by MAP-C06b. The test also adds a fake location of type “Edge A” to each city’s location list; nothing is saved.
  - Steps: Open Tools → Map with the first e2e city open → Click Filter, then the “Edge A” chip → Check that the toolbar button reads “Filter (1)” → Click the floating “Cities” button and pick the second e2e city → Check that the toolbar button reads plain “Filter” again → Switch back to the first e2e city
  - Expected: Switching city clears an active type filter.

- **the last visited city comes back on the next open and is restored** — passed (map.spec.js:1188)
  - Where: Tools → Map, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06, and the second e2e city (from a browser location near Tennant Creek, Australia), added by MAP-C06b.
  - Steps: Open Tools → Map with the first e2e city open → Click the floating “Cities” button and pick the second e2e city → Close and reopen the map → Check that the second e2e city opens by itself → Switch back to the first e2e city → Check that the first city is now remembered as the last visited one
  - Expected: The last visited city opens again on the next visit; the test then switches back to the first city.

- **cancelling Delete City sends nothing and keeps the city** — passed (map.spec.js:1200)
  - Where: Tools → Map → Cities, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06.
  - Steps: Open Tools → Map and click the floating “Cities” button → Click Delete on the e2e city’s row → Check that the Delete City dialog asks “Are you sure you want to delete “<city>”? This will also delete all associated locations.” → Press Cancel → Check that the dialog closes and no delete was sent → Check that the city is still listed and still open
  - Expected: Cancelling Delete City deletes nothing; the city stays listed and open.

- **the keyboard reorders two adjacent e2e cities and puts them back** — passed (map.spec.js:1221)
  - Where: Tools → Map → Cities, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06, and the second e2e city (from a browser location near Tennant Creek, Australia), added by MAP-C06b.
  - Steps: Open Tools → Map and click the floating “Cities” button → Check that the two e2e cities sit next to each other in the list → Focus the drag handle of the upper e2e city with the keyboard → Press Space, then the Down arrow, then Space → Check that the new order is saved and the lower city now shows above the upper one → Focus the drag handle of the city that moved up and press Space, Down arrow, Space → Check that the saved order is exactly the original one again
  - Expected: The keyboard can swap two adjacent cities; the new order is saved, and swapping back restores the original order.

- **a refused reorder puts the order back (the PUT is stubbed)** — passed (map.spec.js:1255)
  - Where: Tools → Map → Cities, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06, and the second e2e city (from a browser location near Tennant Creek, Australia), added by MAP-C06b. The test makes the server refuse the new city order with an error message.
  - Steps: Open Tools → Map and click the floating “Cities” button → Note the order of the cities → Focus the drag handle of the upper e2e city with the keyboard → Press Space, then the Down arrow, then Space → Check that a toast shows the server’s error message → Check that the city list is reloaded and shows the original order
  - Expected: A refused reorder shows the server’s message and the list goes back to the original order.

- **Delete City sends one DELETE however often it is clicked** — passed (map.spec.js:1280)
  - Where: Tools → Map → Cities, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06, and the second e2e city (from a browser location near Tennant Creek, Australia), added by MAP-C06b.
  - Steps: Open Tools → Map and click the floating “Cities” button → Click Delete on the second e2e city’s row → In the Delete City dialog, triple-click Delete → Check that only one delete was sent → Check that a toast says “City deleted successfully” → Open the Cities drawer and check that the second e2e city is gone
  - Expected: Delete City sends exactly one delete however often it is clicked, and the city disappears from the list.

- **deleting the open city opens another one (or the empty state)** — passed (map.spec.js:1295)
  - Where: Tools → Map, on the first e2e city · account: iPhone XRR
  - Setup: Uses the e2e city (named by Google from a browser location near Alice Springs, Australia), added by MAP-C06.
  - Steps: Open Tools → Map with the e2e city open → Click the floating “Cities” button → Click Delete on the e2e city’s row and confirm with Delete → Check that the delete succeeds and the e2e city’s name leaves the toolbar → Check that another city opens by itself, or the floating “Add City” shows when no city is left
  - Expected: Deleting the open city moves the map to another city, or to the no-city state when none is left.

### Map — seeded: types, locations and read views (serial) › locations through pin mode (canvas click)

- **pin mode drops a preview pin; "Add Location" opens the pre-filled form; Save posts it** — passed (map.spec.js:1407)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03.
  - Steps: Open Tools → Map on the e2e city and note the location count next to the city name → Click “Pin New Location”, then click the middle of the map → Check that a preview card shows the point’s coordinates, then close it with its X → Check that the preview disappears and pin mode stays on (the banner is still shown) → Click the map again and press “Add Location” on the new preview card → Check that no “Outside …” question appears and the New Location form opens with the coordinates filled in → Fill in name “[e2e-map] Loc A …”, type “[e2e-map] Type A …”, tick “e2e sub one”, description “[e2e-map] description of A” (type an address if none was filled in) → Press “Save Location” → Check that the location is saved in the e2e city with these values and the coordinates, without photos, and the form closes → Check that the location count goes up by one and pin mode is still on
  - Expected: A map click in pin mode drops a preview pin; “Add Location” opens the form pre-filled with that point, and saving creates the location in the e2e city and raises the count by one.

- **the type picker drives subtypes and the scene field; a "shoot" type saves its scene** — passed (map.spec.js:1450)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03.
  - Steps: Click “Pin New Location”, click the map a little right of and below the middle, and press “Add Location” → Check that the New Location form has no Scene Number field yet → Click the type picker and check that the types are listed A–Z → Pick “[e2e-map] Type A …” and check that its three subtypes show as tick boxes → Pick “[e2e-map] Type Shoot …” and check that the subtypes go and a Scene Number field (“e.g., 12A”) appears → Type a letter in the type picker and erase it, and check that Scene Number hides again → Fill in name “[e2e-map] Loc Shoot …”, pick “[e2e-map] Type Shoot …” again and type “12A” as the scene number → Press “Save Location” → Check that it is saved with Type Shoot, scene number 12A and no subtypes
  - Expected: The type picker is sorted A–Z, a type brings its own subtypes, a “shoot” type shows Scene Number (hidden again when the type is cleared), and the scene number is saved. Recorded: clicking into the picker shows an empty box while the type stays chosen, so clearing it needs a keystroke.

- **an untyped location saves with an empty type** — passed (map.spec.js:1490)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Click “Pin New Location” and click the map a little left of and below the middle → Press “Add Location” on the preview card → Type “[e2e-map] Loc Plain …” as the name and leave the type empty (type an address if none was filled in) → Press “Save Location” → Check that the location is saved with an empty type
  - Expected: A location saved without a type is stored with an empty type.

### Map — seeded: types, locations and read views (serial)

- **seed: the e2e city exists** — passed (map.spec.js:1333)
  - Where: Tools → Map → Cities, with the browser’s location set near Alice Springs, Australia · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Set the browser’s location near Alice Springs → Open Tools → Map and click the floating “Cities” button → Click “Add” on the “Current Location” suggestion card and press “Add City” → Check that the e2e city was created
  - Expected: The e2e city exists before the rest of this suite runs; the other tests in the suite need it.

- **two location types are created, one with an icon and subtypes** — passed (map.spec.js:1335)
  - Where: Tools → Map, on the e2e city → LOC Types · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city and click LOC Types → Click “Add Type” and type “[e2e-map] Type A …” as the type name → Click “Choose an icon” and pick 🏨 → Add the subtypes “e2e sub one” and “e2e sub two” (type each in “Add a subtype...” and press Enter) → Press Create → Check that the type is saved with the 🏨 icon and both subtypes, active → Check that its row shows 🏨 and “e2e sub one” → Click “Add Type”, type “[e2e-map] Type Shoot …” and press Create without picking an icon → Check that it is saved with the default icon “Z” and listed
  - Expected: Both location types are created: one with its icon and subtypes, the other with the default icon “Z”.

- **editing a type asks first; Cancel keeps the form, Update saves the new subtype** — passed (map.spec.js:1355)
  - Where: Tools → Map, on the e2e city → LOC Types · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the type “[e2e-map] Type A …” created by MAP-J02.
  - Steps: Open Tools → Map on the e2e city and click LOC Types → Click the Edit (pencil) icon on “[e2e-map] Type A …” → Check that the form says “Edit Type — [e2e-map] Type A …” and the row shows “Editing” → Add the subtype “e2e sub three” and press Update → Check that the “Update Location Type” dialog warns it will change the type classification for associated locations → Press Cancel and check that nothing is saved and the edit form stays open → Press Update again, then Update in the dialog → Check that the type is saved with all three subtypes and a toast says “Location type updated successfully” → Check that the row now shows “e2e sub three”
  - Expected: Editing a type asks for confirmation first; Cancel keeps the form unsaved, Update saves the new subtype.

- **the type filter matches subtypes and names** — passed (map.spec.js:1390)
  - Where: Tools → Map, on the e2e city → LOC Types · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (with subtype “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 and MAP-J03.
  - Steps: Open Tools → Map on the e2e city and click LOC Types → Type “e2e sub three” in “Filter by type or subtype...” → Check that “[e2e-map] Type A …” is listed and “[e2e-map] Type Shoot …” is not → Replace the filter with the full name of “[e2e-map] Type Shoot …” → Check that “[e2e-map] Type Shoot …” is listed and “[e2e-map] Type A …” is not
  - Expected: The type filter matches both subtype names and type names.

- **the header counts the seeded locations and the List View badge matches** — passed (map.spec.js:1500)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city → Check that the city’s locations include Loc A, Loc Shoot and Loc Plain → Add up the city’s map locations and any Location-tool pins inside it → Check that the count next to the city name reads “N locations” with that total → Check that the List View button shows the same number
  - Expected: The header count and the List View number both equal the seeded city’s locations (plus any Location-tool pins inside it).

- **search matches name and type, case-insensitive** — passed (map.spec.js:1511)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city and click Search → Type the full name of “[e2e-map] Loc A …” in capital letters → Check that exactly one result shows and it mentions “[e2e-map] Type A …” → Replace the text with the name of the type “[e2e-map] Type Shoot …” → Check that “[e2e-map] Loc Shoot …” is among the results
  - Expected: The location search matches names regardless of upper or lower case, and also matches a location by its type name.

- **a search result opens the card with type, icon, subtype, description and coordinates** — passed (map.spec.js:1522)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city and click Search → Type “[e2e-map] Loc A …” and click the result → Check that the card shows “[e2e-map] Type A …” with its 🏨 icon → Check that it shows the subtype “e2e sub one” and the description “[e2e-map] description of A” → Check that it shows the coordinates with six decimals → Check that it has Edit, Share, View and Directions buttons
  - Expected: The card of a seeded location shows its type with icon, subtype, description, coordinates and the four actions.

- **the type filter: icon and count per chip, "k / n" in the header, Unknown, Clear All** — passed (map.spec.js:1536)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city and wait until the location count next to the city name shows all of the city’s locations (N) → Click Filter → Check that the “[e2e-map] Type A …” chip shows its 🏨 icon and a count of 1 → Click that chip → Check that the toolbar reads “Filter (1)” and the header count reads “1 / N” → Click the “Unknown” chip → Check that the toolbar reads “Filter (2)” and the header count reads “2 / N” → Click “Clear All” → Check that the toolbar reads plain “Filter” again
  - Expected: Filter chips show each type’s icon and count; picking chips narrows the header to “shown / total”, untyped locations sit under “Unknown”, and Clear All removes the filter.

- **a filter that hides the open pin closes its card** — passed (map.spec.js:1561)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city → Search for “[e2e-map] Loc A …” and open its card → Click Filter → Click the “[e2e-map] Type Shoot …” chip → Check that the Loc A card closes
  - Expected: A filter that hides the location whose card is open closes that card.

- **picking a search result hidden by the filter clears the filter** — passed (map.spec.js:1570)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city → Click Filter and click the “[e2e-map] Type Shoot …” chip → Check that the toolbar reads “Filter (1)” → Search for “[e2e-map] Loc A …” (a Type A location) and click the result → Check that the toolbar reads plain “Filter” again → Check that the Loc A card is shown
  - Expected: Picking a search result that the active filter hides clears the filter and opens the card.

- **Edit on the card opens the drawer filled from the record; Cancel sends nothing** — passed (map.spec.js:1581)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city → Search for “[e2e-map] Loc A …” and open its card → Click Edit on the card → Check that the Edit Location form shows the name, the type “[e2e-map] Type A …”, the ticked subtype “e2e sub one”, the description and an address → Press Cancel → Check that the form closes and nothing was saved
  - Expected: Edit on the card opens the Edit Location form filled from the saved location, and Cancel saves nothing.

- **"View" opens the Locations page in the same window; "Back to Map" returns** — passed (map.spec.js:1599)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city → Search for “[e2e-map] Loc A …” and open its card → Click View on the card → Check that the Locations page opens in the same window (not the List View drawer) → Check that it lists “[e2e-map] Loc A …” and shows a “Map View” button → Click the back arrow “Back to Map” → Check that the map is back on the e2e city
  - Expected: “View” opens the Locations page inside the Map window, and “Back to Map” returns to the same city.

- **the List View cards show type, scene, subtypes and the per-card actions** — passed (map.spec.js:1613)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city and click List View → Check that there is one card per location and the count reads “N Locations” → Check that the Loc A card shows “[e2e-map] Type A …” and “e2e sub one” → Check that the Loc Shoot card shows “SC 12A” → Check that the Loc Plain card shows no “Unknown” label → Check that the Loc Plain card has exactly three buttons: View, Edit and the trash icon
  - Expected: List View cards show each location’s type, scene number and subtypes, and offer View, Edit and Delete.

- **the List View type tabs filter the cards and update the subtitle** — passed (map.spec.js:1632)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city and click List View → Check that the “All” tab shows the number of locations (N) → Click the “[e2e-map] Type A …” tab → Check that one card shows and the subtitle reads “Showing 1 of N locations” → Click the “Unknown” tab → Check that only the “[e2e-map] Loc Plain …” card shows → Click “All” and check that all N cards show again
  - Expected: The List View type tabs filter the cards (untyped ones under “Unknown”) and the subtitle says how many are shown.

- **the detail drawer shows the record and copies the coordinates** — passed (map.spec.js:1649)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city and click List View → Click View on the “[e2e-map] Loc A …” card → Check that the detail drawer has the sections Location Details, Subtypes and Description → Check that the coordinates show as “latitude, longitude” with six decimals → Click “Copy coordinates” → Check that a message says “Coordinates copied” and the button reads “Copied” → Paste the clipboard and check that it holds the same coordinates → Close the drawer
  - Expected: The detail drawer shows the location’s record, and the copy button puts its coordinates on the clipboard.

- **"Add" in the type picker creates a type and selects it** — passed (map.spec.js:1668)
  - Where: Tools → Map, on the e2e city → List View → New Location · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city, click List View, then “New Location” → Click the type picker and type “[e2e-map] Inline Type …” → Check that it says “No types found” → Click the add button “Create new location type” → Check that the New Type dialog opens with the name already filled in → Press Create → Check that the type is saved, the dialog closes and the picker now shows the new type → Press Cancel on the location form and check that no location was saved
  - Expected: A type can be created straight from the type picker, and it is then selected in the form.

- **a viewer (no posting right) is asked to request rights instead of editing** — passed (map.spec.js:1694)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b. The test makes the rights reply say the account may view but not post on Map.
  - Steps: Open Tools → Map on the e2e city as a user without the Map posting right → Open the Cities drawer: check there is no “Add Another City”; click Delete on the e2e city, check a prompt asks to request rights, answer No, and check no Delete City dialog opens → Click “Pin New Location”, answer No to the prompt, and check pin mode stays off → Search for “[e2e-map] Loc A …”, open its card, click Edit, answer No, and check no Edit Location form opens → Open List View and check the Loc A card shows only View → Click “New Location” and answer No to the prompt → Click View on the Loc A card, click Edit in the detail, answer No, and check no form opens and the detail closes → Open LOC Types, click “Add Type” and answer No → Click Edit, then Delete, on “[e2e-map] Type A …”, answering No each time, and check neither the edit form nor the Delete Location Type dialog opens
  - Expected: A user without the Map posting right sees no “Add Another City” and no Edit / Delete on List View cards; every other write asks to request rights instead, and answering No changes nothing.

- **an edit from the List View is a full PUT of the record** — passed (map.spec.js:1738)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city and click List View → Click Edit on the “[e2e-map] Loc Shoot …” card → Check that the Scene Number field shows “12A” → Replace the description with “[e2e-map] edited …” → Press “Update Location” → Check that the whole record is saved: city, name, type “[e2e-map] Type Shoot …”, scene 12A, the new description and the photo list → Check that the form closes and the reloaded location shows the new description
  - Expected: Editing from the List View saves the complete location record with the change, keeping type, scene number and photos.

- **a photo added on edit uploads to S3 and is saved as an attachment** — passed (map.spec.js:1767)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city and click List View → Click Edit on the “[e2e-map] Loc A …” card → Add one small PNG image as a photo and check a “New” preview shows → Press “Update Location” → Check that the photo uploads and the location is saved with exactly one attachment → Check that the form closes → Click View on the Loc A card and check the detail shows “Photos (1)”
  - Expected: A photo added while editing is uploaded and saved with the location, and the detail shows “Photos (1)”.

- **removing the saved photo sends an empty attachments list** — passed (map.spec.js:1794)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b. Uses the photo MAP-F06 saved on Loc A.
  - Steps: Open Tools → Map on the e2e city and click List View → Click Edit on the “[e2e-map] Loc A …” card → Click Remove on the saved photo → Press “Update Location” → Check that the location is saved with no photos
  - Expected: Removing the saved photo saves the location with an empty photo list.

- **Delete Location sends one DELETE however often it is clicked** — passed (map.spec.js:1810)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and the locations “[e2e-map] Loc A …” (Type A, subtype “e2e sub one”, description “[e2e-map] description of A”), “[e2e-map] Loc Shoot …” (Type Shoot, scene 12A) and “[e2e-map] Loc Plain …” (no type) pinned by MAP-G02/G03, MAP-F03 and MAP-G03b.
  - Steps: Open Tools → Map on the e2e city and click List View → Click the trash icon on the “[e2e-map] Loc Plain …” card → Check that the Delete Location dialog asks “Are you sure you want to delete “[e2e-map] Loc Plain …”?” → Press Cancel → Click the trash icon again and triple-click Delete → Check that only one delete was sent → Check that the Loc Plain card is gone
  - Expected: Delete Location asks first, sends exactly one delete however often it is clicked, and removes the card.

- **the e2e location types are deleted** — passed (map.spec.js:1826)
  - Where: Tools → Map, on the e2e city → LOC Types · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the types “[e2e-map] Type A …” (icon 🏨, subtypes “e2e sub one”, “e2e sub two”, “e2e sub three”) and “[e2e-map] Type Shoot …” from MAP-J02 / MAP-J03, and “[e2e-map] Inline Type …” from MAP-F04.
  - Steps: Open Tools → Map on the e2e city and click LOC Types → Click the trash icon on each “[e2e-map]” type this suite created → Confirm with Delete in the Delete Location Type dialog → Check that a toast says “Location type deleted successfully” each time → Check that each type’s row disappears
  - Expected: Every e2e location type created by the suite is deleted, each with a success toast.

### Map — seeded: studio zones (serial) › zones on the canvas

- **a pin outside the active zone asks "Outside Studio Zone"; No aborts, Yes opens the form** — passed (map.spec.js:2051)
  - Where: Tools → Map, on the e2e city, with the 1-mile zone drawn · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zones “[e2e-map] Zone One …” (40 miles, created by MAP-I05) and “[e2e-map] Zone Tiny …” (1 mile, created by MAP-I05b).
  - Steps: Open Tools → Map on the e2e city with “[e2e-map] Zone Tiny …” as the drawn zone (pick it with “Map” in Studio Zones) → Click “Pin New Location” and check the banner reads “Click inside “[e2e-map] Zone Tiny …” to place a pin (restricted to zone)” → Click the map near its left edge, in the lower third (outside the 1-mile circle) → Press “Add Location” on the preview card → Check that the “Outside Studio Zone” dialog asks “Location is outside the <zone>. Do you want to pin the location?” → Press No and check that the dialog closes and no form opens → Press “Add Location” again and answer Yes → Check that the New Location form opens, then press Cancel
  - Expected: A pin outside the drawn zone asks “Outside Studio Zone”: No cancels it, Yes opens the New Location form.

- **a click on the drawn zone opens its card; "Edit Zone" opens the zone form** — passed (map.spec.js:2078)
  - Where: Tools → Map, on the e2e city, with the 50-mile zone drawn · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zones “[e2e-map] Zone One …” (40 miles, created by MAP-I05) and “[e2e-map] Zone Tiny …” (1 mile, created by MAP-I05b). “[e2e-map] Zone One …” was widened to 50 miles by MAP-I08.
  - Steps: Open Tools → Map on the e2e city with “[e2e-map] Zone One …” as the drawn zone → Click the map just off its middle, inside the drawn circle → Check that the zone’s card opens with its name → Check that it says “Radius: 50 mi (80.5 km)” → Click “Edit Zone” → Check that the Edit Studio Zone form opens, then press Cancel
  - Expected: A click on the drawn zone opens its card with the radius in miles and km, and “Edit Zone” opens the zone form.

### Map — seeded: studio zones (serial)

- **seed: the e2e city exists** — passed (map.spec.js:1829)
  - Where: Tools → Map → Cities, with the browser’s location set near Alice Springs, Australia · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Set the browser’s location near Alice Springs → Open Tools → Map and click the floating “Cities” button → Click “Add” on the “Current Location” suggestion card and press “Add City” → Check that the e2e city was created
  - Expected: The e2e city exists before the rest of this suite runs; the other zone tests need it.

- **a city without zones: the empty drawer, re-read twice on open** — passed (map.spec.js:1831)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the new e2e city → Check that the toolbar zone button reads plain “Studio Zone” → Click it and check that the zones are loaded again (twice) as the drawer opens → Check that the drawer says “No studio zones yet” and offers “Add Studio Zone” → Check that the subtitle reads “<city> · Activate a zone to view it on the map” → Check that it shows “0 Zones” and no zone filter box
  - Expected: A city without zones shows the empty Studio Zones drawer with “Add Studio Zone”, “0 Zones” and no filter box.

- **a zone is created at the city centre with the chosen radius** — passed (map.spec.js:1846)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city and click the Studio Zone button → Click “Add Zone” → Check that the New Studio Zone form shows “New entry”, the Zone Name is pre-filled with the city name and “30 mi” is selected → Check that it says “This zone covers a 30-mile radius around the center point.” → Replace the Zone Name with “[e2e-map] Zone One …” → Click “40 mi” and check the line now says a 40-mile radius → Press “Create Zone” → Check that the zone is saved for the e2e city as a 40-mile studio zone centred on the city’s point → Check that its row shows “40 mi radius”
  - Expected: A new zone defaults to the city’s name and 30 miles, and is created at the city centre with the chosen 40-mile radius.

- **a city with zones always draws one: the toolbar names it on open** — passed (map.spec.js:1884)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zone “[e2e-map] Zone One …” created by MAP-I05.
  - Steps: Open Tools → Map on the e2e city → Look at the zone button in the toolbar → Check that it reads “Zone: [e2e-map] Zone One …”
  - Expected: A city that has a zone always draws one on open, and the toolbar names it.

- **a Custom radius must be filled and positive; nothing is sent until it is** — passed (map.spec.js:1890)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city, click the Studio Zone button, then “Add Zone” → Click Custom and press “Create Zone” with the radius empty → Check that a toast and a note under the field both say “Zone radius is required when Custom is selected.” → Type 0 in “Enter radius in miles” and leave the field → Check that the note says “Enter a radius greater than 0 miles.” → Click “30 mi” → Check that the note disappears → Press Cancel and check that nothing was saved
  - Expected: A Custom radius must be filled and above 0; the form explains each problem and sends nothing.

- **an emptied Zone Name blocks the submit natively** — passed (map.spec.js:1912)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city, click the Studio Zone button, then “Add Zone” → Clear the Zone Name field → Press “Create Zone” → Check that the browser marks Zone Name as a required field and nothing is saved → Check that the form stays open → Press Cancel
  - Expected: An empty Zone Name blocks the save with the browser’s required-field check. Recorded: the “<city> Zone” default name is therefore never used.

- **a one-mile Custom zone is created** — passed (map.spec.js:1931)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city, click the Studio Zone button, then “Add Zone” → Type “[e2e-map] Zone Tiny …” as the Zone Name → Click Custom and type 1 in “Enter radius in miles” → Press “Create Zone” → Check that the zone is saved as a 1-mile studio zone → Check that its row shows “1 mi radius”
  - Expected: A one-mile Custom zone is created.

- **the zone filter matches names and says when nothing matches** — passed (map.spec.js:1944)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zones “[e2e-map] Zone One …” (40 miles, created by MAP-I05) and “[e2e-map] Zone Tiny …” (1 mile, created by MAP-I05b).
  - Steps: Open Tools → Map on the e2e city and click the Studio Zone button → Type the name of “[e2e-map] Zone Tiny …” in “Filter by zone name or address...” → Check that “[e2e-map] Zone Tiny …” is listed and “[e2e-map] Zone One …” is not → Type “zz-e2e-none” → Check that it says “No zones match “zz-e2e-none”” → Clear the filter → Check that “[e2e-map] Zone One …” is listed again
  - Expected: The zone filter matches zone names and says when nothing matches.

- **"Map" switches the drawn zone, remembers it per city, and is restored** — passed (map.spec.js:1958)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zones “[e2e-map] Zone One …” (40 miles, created by MAP-I05) and “[e2e-map] Zone Tiny …” (1 mile, created by MAP-I05b).
  - Steps: Open Tools → Map on the e2e city and click the Studio Zone button → Click “Map” on “[e2e-map] Zone Tiny …” → Check that the drawer closes and the toolbar reads “Zone: [e2e-map] Zone Tiny …” → Check that the app remembers this zone for this city → Open the drawer again and check that “[e2e-map] Zone Tiny …” is marked “Active” and the drawer says it “is showing on the map” → Click “Map” on “[e2e-map] Zone One …” → Check that the toolbar reads “Zone: [e2e-map] Zone One …”
  - Expected: “Map” switches which zone is drawn, the choice is remembered for the city and marked Active, and switching back restores the first zone.

- **the zone picked with "Map" is still drawn after a reopen** — passed (map.spec.js:1975)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zones “[e2e-map] Zone One …” (40 miles, created by MAP-I05) and “[e2e-map] Zone Tiny …” (1 mile, created by MAP-I05b).
  - Steps: Open Tools → Map on the e2e city and click the Studio Zone button → Click “Map” on “[e2e-map] Zone Tiny …” → Close and reopen the map → Check that the toolbar still reads “Zone: [e2e-map] Zone Tiny …” → Open the Studio Zones drawer and click “Map” on “[e2e-map] Zone One …” → Check that the toolbar reads “Zone: [e2e-map] Zone One …”
  - Expected: The zone picked with “Map” is still drawn after the map is reopened.

- **Details shows the zone, its radius and a preview** — passed (map.spec.js:1988)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zones “[e2e-map] Zone One …” (40 miles, created by MAP-I05) and “[e2e-map] Zone Tiny …” (1 mile, created by MAP-I05b).
  - Steps: Open Tools → Map on the e2e city and click the Studio Zone button → Click Details on “[e2e-map] Zone One …” → Check that a drawer titled with the zone’s name opens and shows “40 mi radius” → Check that it has the sections Zone Details, Radius, Center Point and Zone Preview → Check that it has an Edit button → Close the drawer
  - Expected: The zone’s Details drawer shows its radius, details, centre point, a preview and an Edit button.

- **Edit re-reads the zones, shows the saved radius and PUTs the new one** — passed (map.spec.js:2003)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zones “[e2e-map] Zone One …” (40 miles, created by MAP-I05) and “[e2e-map] Zone Tiny …” (1 mile, created by MAP-I05b).
  - Steps: Open Tools → Map on the e2e city and click the Studio Zone button → Click Edit on “[e2e-map] Zone One …” → Check that the zones are loaded again and the Edit Studio Zone form shows “Editing” → Check that the Zone Name is filled in and “40 mi” is selected → Click “50 mi” and press “Update Zone” → Check that the same zone is saved with the same name and a 50-mile radius → Check that its row shows “50 mi radius”
  - Expected: Editing a zone opens it with its saved radius and saves the new 50-mile radius.

- **a viewer sees no zone Edit / Delete and is asked to request rights** — passed (map.spec.js:2029)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zones “[e2e-map] Zone One …” (40 miles, created by MAP-I05) and “[e2e-map] Zone Tiny …” (1 mile, created by MAP-I05b). The test makes the rights reply say the account may view but not post on Map.
  - Steps: Open Tools → Map on the e2e city as a user without the Map posting right → Click the Studio Zone button → Check that “[e2e-map] Zone One …” shows only the Map and Details buttons (no Edit or Delete) → Click “Add Zone”, check that a prompt asks to request rights, answer No, and check no zone form opens → Click Details on “[e2e-map] Zone One …”, then Edit in the detail → Check that a prompt asks to request rights, answer No, and check no Edit Studio Zone form opens
  - Expected: A user without the Map posting right sees only Map and Details on zones; Add Zone and the detail’s Edit ask to request rights instead.

- **Delete Studio Zone removes the zone after a confirm** — passed (map.spec.js:2096)
  - Where: Tools → Map, on the e2e city → Studio Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. Uses the zones “[e2e-map] Zone One …” (40 miles, created by MAP-I05) and “[e2e-map] Zone Tiny …” (1 mile, created by MAP-I05b).
  - Steps: Open Tools → Map on the e2e city and click the Studio Zone button → Click the trash icon on “[e2e-map] Zone Tiny …” → Check that the Delete Studio Zone dialog asks “Are you sure you want to delete “<zone>”?” → Press Cancel → Click the trash icon again and press Delete → Check that the zone is deleted and its row disappears → Check that the drawer now shows “1 Zone”
  - Expected: Delete Studio Zone asks first, then removes the zone, leaving one zone.

### Map — seeded: markers, hover and drag (canvas)

- **seed: the e2e city exists** — passed (map.spec.js:2124)
  - Where: Tools → Map → Cities, with the browser’s location set near Alice Springs, Australia · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Set the browser’s location near Alice Springs → Open Tools → Map and click the floating “Cities” button → Click “Add” on the “Current Location” suggestion card and press “Add City” → Check that the e2e city was created
  - Expected: The e2e city exists before the marker, hover and drag tests run.

- **at zoom 16 the marker is a DOM node; clicking it opens the card** — passed (map.spec.js:2126)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. The test then makes sure the city has a location “[e2e-map] Canvas pin …”, creating it once if needed with “Pin New Location”, a click in the middle of the map, “Add Location” and “Save Location”.
  - Steps: Open Tools → Map on the e2e city → Click Search, type “[e2e-map] Canvas pin …” and click the result → Check that the map zooms in and the location’s marker is on the map → Close the location’s card with its X → Click the marker on the map → Check that the location’s card opens again
  - Expected: Once zoomed in, the location’s marker is on the map, and clicking it opens its card.

- **hovering a marker previews its card; leaving hides it** — passed (map.spec.js:2142)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. The test then makes sure the city has a location “[e2e-map] Canvas pin …”, creating it once if needed with “Pin New Location”, a click in the middle of the map, “Add Location” and “Save Location”.
  - Steps: Open Tools → Map on the e2e city → Search for “[e2e-map] Canvas pin …”, open its card, then close it with its X → Hover the mouse over the location’s marker → Check that its card appears as a preview within a few seconds → Move the mouse to the lower-left corner of the map → Check that the preview card disappears
  - Expected: Hovering a marker shows its card as a preview; moving away hides it.

- **dragging a pin in pin mode asks "Move Location"; Cancel keeps it, Move PUTs the whole record** — passed (map.spec.js:2158)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. The test then makes sure the city has a location “[e2e-map] Canvas pin …”, creating it once if needed with “Pin New Location”, a click in the middle of the map, “Add Location” and “Save Location”.
  - Steps: Open Tools → Map on the e2e city → Search for “[e2e-map] Canvas pin …”, open its card, then close it → Click “Pin New Location” to turn on pin mode → Drag the location’s marker a short way down and to the right → Check that the “Move Location” dialog asks “Move ‘<name>’ to the new position?” → Press Cancel and check that nothing is saved → Drag the marker again and press Move → Check that the whole location record is saved (same location, name and city) at the new point → Check that a toast says “‘<name>’ moved”
  - Expected: Dragging a pin in pin mode asks “Move Location”: Cancel keeps it in place, Move saves the full record at the new spot and confirms.

### Map — seeded: Google Places, boundaries and directions

- **seed: the e2e city exists** — passed (map.spec.js:2248)
  - Where: Tools → Map → Cities, with the browser’s location set near Alice Springs, Australia · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Set the browser’s location near Alice Springs → Open Tools → Map and click the floating “Cities” button → Click “Add” on the “Current Location” suggestion card and press “Add City” → Check that the e2e city was created
  - Expected: The e2e city exists before the Google Places, boundary and directions tests run.

- **a Places result for the city pins inside it; the searched pin clears after the save** — passed (map.spec.js:2250)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city → Click “Search places on map” and type the e2e city’s name in “Search any place...” → Click the first Google suggestion → Check that the search box closes and a place card shows with “Pin Location” and “Directions” → Click “Pin Location” → Check that no “Outside …” question appears and the New Location form opens → Type “[e2e-map] Places pin …” as the name (and an address if none was filled in), then press “Save Location” → Check that the location is saved and the form closes → Check that the place card and the searched place’s marker are gone
  - Expected: A place found inside the city can be pinned straight away, and after the save the searched place’s card and marker are cleared.

- **a far place asks "Outside <city>" with three choices; Cancel aborts, "same city" opens the form** — passed (map.spec.js:2276)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city → Click “Search places on map”, type “Uluru” and click the first suggestion → Click “Pin Location” on the place card → Check that the “Outside <city>” dialog says “This location is outside the selected city and studio zone. What would you like to do?” → Check that it offers “Create Another City & Pin Location”, “Pin Location in the Same City” and Cancel → Press Cancel and check that no form opens → Click “Pin Location” again and choose “Pin Location in the Same City” → Check that the New Location form opens with Uluru’s coordinates, far from the city → Press Cancel and check that nothing was saved
  - Expected: Pinning a place far outside the city asks what to do with three choices; Cancel stops, and “Pin Location in the Same City” opens the form at the far point.

- **"Create Another City & Pin Location": Cancel drops the pending pin; Add City reopens the form there** — skipped (map.spec.js:2306)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city → Search places for “Uluru”, pick the first suggestion and click “Pin Location” → Choose “Create Another City & Pin Location” → Check that the Add City dialog opens with the city already chosen (no city search box), then press Cancel → Check that the pending pin is dropped → Click “Pin Location” and choose “Create Another City & Pin Location” again → Note the proposed city name and press “Add City” → Check that the city is created, opens in the toolbar, and the New Location form opens at the place → Type “[e2e-map] Remote pin …” as the name (and an address if none was filled in), then press “Save Location” → Check that the location is saved in the new city
  - Expected: “Create Another City & Pin Location” creates the new city, switches to it and opens the form at the place; Cancel drops the pending pin. Recorded: a place with no town is named by its Google plus code (for example “5Q6HM24P+6Q”).

- **picking an existing city in the drawer search selects it without a form** — passed (map.spec.js:2361)
  - Where: Tools → Map, on the e2e city → Cities · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city and click the floating “Cities” button → Type the e2e city’s name in “Search cities...” → Click the first Google suggestion → Check that the drawer closes and no Add City dialog opens → Check that the e2e city is open and no new city was created
  - Expected: Picking a city that already exists from the drawer’s search simply selects it, without a form or a new city.

- **Add City refuses a name that already exists** — passed (map.spec.js:2374)
  - Where: Tools → Map, on the e2e city → Cities · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city and click the floating “Cities” button → Click “Add Another City” → Type the e2e city’s name in “Search for a city...” and click the first suggestion → Check that the coordinates fill in → Press “Add City” → Check that a toast says the city “already exists. Please use a different city name.” → Press Cancel and check that no city was created
  - Expected: Add City refuses a city name that already exists, with a message, and creates nothing.

- **a city picked in the Add City search is created and opened** — skipped (map.spec.js:2390)
  - Where: Tools → Map, on the e2e city → Cities · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city and click the floating “Cities” button → Click “Add Another City” → Type “Katherine NT” in “Search for a city...” and click the first suggestion → Note the city name shown and check that the coordinates fill in → Press “Add City” → Check that the city is created and a toast says “City added successfully” → Check that the new city opens in the toolbar
  - Expected: A city picked in the Add City search is created and opened. Recorded: if Google names the place by a plus code.

- **an address outside the city's zones asks window.confirm in the map's drawer** — passed (map.spec.js:2421)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. The test adds a 3-mile Custom zone “[e2e-map] F08 zone …” to the city.
  - Steps: Open Tools → Map on the e2e city; in Studio Zone add the 3-mile zone “[e2e-map] F08 zone …” → Reopen the map, search places for the e2e city’s name and click “Pin Location” (answer Yes if “Outside Studio Zone” appears) → In the New Location form note the latitude → In “Search address...” type “Alice Springs Airport” and click the first suggestion → Check that a browser pop-up asks whether to use the address although it is outside (“Do you want to use it anyway?”) and refuse it → Check that the latitude did not change → Pick “Alice Springs Airport” again and accept the pop-up → Check that the latitude changes to the new address → Press Cancel
  - Expected: An address outside the city’s zones asks for confirmation: refusing keeps the old point, accepting uses the new address. Recorded: the question is a plain browser pop-up, which the app’s own rules do not allow.

- **street intersection and centre-point checks in the zone form send nothing** — passed (map.spec.js:2468)
  - Where: Tools → Map, on the e2e city → Studio Zone → Add Zone · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. The test answers any zone save itself with a refusal, so nothing is saved even if the checks let one through.
  - Steps: Open Tools → Map on the e2e city, click the Studio Zone button, then “Add Zone” → Click “Street Intersection” and check that “Select Intersection” is disabled → Type “Todd” in Street 1 without picking a suggestion and click “Select Intersection” → Check that a toast says “Please select Street 1 from the dropdown suggestions” → Pick “Todd Street” for Street 1 and “Gregory Terrace” for Street 2 from the suggestions, then click “Select Intersection” → Check that “Intersection Found” shows and the Zone Name becomes “<street> & <street> Zone” (if Google finds the crossing) → Click “Center Point”, pick the “Alice Springs Airport” suggestion and check that the Zone Name changes to the airport’s name → Click Custom, type 1 as the radius and press “Create Zone” → Check that a toast says the city is a number of miles from the center point, but the zone radius is 1 miles → Press Cancel and check that no save was sent
  - Expected: The zone form refuses a typed (unpicked) street, finds the intersection of two picked streets and names the zone after it, and refuses a centre farther away than the radius; nothing is saved.

- **directions to a place: route, new pickup, share, Call a Driver (stubbed)** — passed (map.spec.js:2516)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion. The test answers the ride request itself, so no real trip is created.
  - Steps: Open Tools → Map on the e2e city, search places for “Alice Springs Airport” and pick the first suggestion → Click Directions on the place card → Check that the Get Directions panel fills the destination and, from the browser’s location, the pickup → Check that it shows Distance in miles and Duration → Click Share in the panel, check that the share menu opens, then click Share again to close it → Check that “Use my current location” is on; pick the e2e city as the pickup from the suggestions and check it turns off; click it and check it turns on again → Click “Call a Driver”, then “Call a Driver” in the drawer that opens → Check that a toast says “Trip created successfully” and one ride request with a pickup and a drop-off was sent → Check that the directions panel closes
  - Expected: Directions to a place show the route’s distance and duration, can be shared, switch between “my location” and a typed pickup, and Call a Driver sends one ride request and closes the panel.

- **closing directions from a searched place also removes its pin** — passed (map.spec.js:2563)
  - Where: Tools → Map, on the e2e city · account: iPhone XRR
  - Setup: Before the suite: removes leftovers of earlier runs, then adds the e2e city (named by Google from a browser location near Alice Springs, Australia) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Open Tools → Map on the e2e city → Search places for “Alice Springs Airport” and pick the first suggestion → Click Directions on the place card → Check that the Get Directions panel opens → Close the panel with its X → Check that the panel, the place card and the searched place’s marker are all gone
  - Expected: Closing directions that started from a searched place also removes that place’s pin and card.

### Map — seeded: Classic view sub-routes

- **seed: the e2e city exists** — passed (map.spec.js:2554)
  - Where: Tools → Map, with the browser location set near Alice Springs · account: iPhone XRR
  - Setup: Before the group runs: removes any leftover e2e data, then adds a throwaway city (named by Google, e.g. Alice Springs) from the Cities drawer's “Current Location” suggestion at an emulated location near Alice Springs.
  - Steps: Open the Cities drawer → Click Add on the “Current Location” suggestion card → Click “Add City” in the Add City dialog → Check that the city was saved without an error
  - Expected: The throwaway e2e city exists, so the other tests of the group can use it.

- **the full-page Add Studio Zone form creates a zone and returns to the list** — passed (map.spec.js:2556)
  - Where: Classic view (sidebar “Classic view”) → the Map's full-page Add Studio Zone form, reached by a direct link, with the e2e city as the last visited city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00c).
  - Steps: Switch to Classic view and open the full-page Add Studio Zone form → Check that Zone Name is pre-filled with the e2e city's name → Replace Zone Name with “[e2e-map] Classic zone <time>” → Click “Create Studio Zone” → Check that the save succeeds → Check that the Studio Zones page opens and lists the new zone → Click the zone's Delete button and press Delete in “Delete Studio Zone” → Check that the delete succeeds
  - Expected: The full-page form creates the zone and returns to the Studio Zones page, where the new zone is listed and can be deleted.

- **the full-page New Location form takes lat / lng from the URL and saves** — passed (map.spec.js:2584)
  - Where: Classic view (sidebar “Classic view”) → the Map's full-page New Location form, opened by a direct link that carries a latitude and longitude just north of the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00c), set as the last visited city.
  - Steps: Switch to Classic view and open the New Location form with coordinates in the link → Type “[e2e-map] Classic location <time>” in Location Name → Wait for the address to fill in by itself; if it stays empty, type “[e2e-map] address” → Click “Save Location” → Check that the save succeeds with the latitude from the link and the e2e city → Check that the Locations page opens and shows the new location's card → Delete the card (trash button, then Delete in “Delete Location”) → Check that the delete succeeds
  - Expected: The location is saved in the e2e city at the coordinates from the link, and the Locations page then lists its card.

### Map — Classic view sub-routes (read-only)

- **a cold Locations page can open its New Location form** — bug · known bug the Locations page has no GoogleMapsLoader: opened cold, "New Location" crashes into the ErrorBoundary ("Something went wrong.", code /film-tools/map/locations-…) — confirmed run 1,the Locations page has no GoogleMapsLoader: opened cold, "New Location" crashes into the ErrorBoundary ("Something went wrong.", code /film-tools/map/locations-…) — confirmed run 1 (map.spec.js:2626)
  - Where: Classic view (sidebar “Classic view”) → the Map's Locations page, opened directly by link without opening the map first · account: iPhone XRR
  - Steps: Switch to Classic view and open the Map's Locations page directly → Check that “Back to Map” is shown → Wait for “Loading locations...” to go away → Click “New Location” → Check that the New Location form opens with its “Search address...” field → Check that no “Something went wrong” page shows → Click Cancel
  - Expected: The New Location form opens and works on a Locations page opened cold. Known bug: the page crashes into “Something went wrong.” because the Locations page does not load Google Maps by itself (confirmed on a live run).
  - Web error: Error: expect(locator).toBeVisible() failed

- **Edit on the Studio Zones page opens the zone form** — skipped (map.spec.js:2643)
  - Where: Classic view (sidebar “Classic view”) → the Map's Studio Zones page, opened directly by link, for a city that has studio zones · account: iPhone XRR
  - Steps: Switch to Classic view and open the Map's Studio Zones page directly → Wait for “Loading studio zones...” to go away → Check that “Map View” is shown → Click Edit on the first zone → Check that the “Edit Studio Zone” form opens
  - Expected: Edit opens the Edit Studio Zone form. Known bug: Edit goes to a page the app does not have, so no form opens.

### Map — known bugs (correct behaviour asserted) › on the canvas

- **a List View edit shows on the map card at once** — passed · known bug the List View drawer keeps its own list: an edit there does not refresh MapView, whose card shows the old description — NOT reproduced in run 1 (passed; own-device socket echo in the seeded session),the List View drawer keeps its own list: an edit there does not refresh MapView, whose card shows the old description — NOT reproduced in run 1 (passed; own-device socket echo in the seeded session) (map.spec.js:2739)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00k). If the group's test pin is missing, creates “[e2e-map] KB pin <time>”: Pin New Location, click the map centre, Add Location, Save Location, then leave pin mode.
  - Steps: Click List View → Click Edit on the test pin's card → In “Edit Location” type a new description “[e2e-map] kb <time>” → Click “Update Location” and check that the save succeeds → Close the List View drawer → Click Search, type the pin's name and pick the result → Check that the map card shows the new description
  - Expected: The map card shows the description edited in the List View at once. Known bug: the List View keeps its own list, so the map card can show the old description (not reproduced on run 1; re-check in a real QR-code session).

- **a List View delete updates the header count** — passed · known bug MapView keeps its own list: a delete in the List View drawer leaves the header count and the marker stale (not confirmed: run 1 failed on the duplicate card, run 3 on a stale read — the header had dropped),MapView keeps its own list: a delete in the List View drawer leaves the header count and the marker stale (not confirmed: run 1 failed on the duplicate card, run 3 on a stale read — the header had dropped) (map.spec.js:2788)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00k).
  - Steps: Click “Pin New Location” and click the map a little up and right of the centre → Click “Add Location”, type “[e2e-map] KB delete me <time>” as the name and click “Save Location” → Leave pin mode → Click List View and wait until its list shows the new location → Delete the first card with that name (trash button, then Delete in “Delete Location”) and check that it succeeds → Wait until the city’s locations are read again without the deleted one → Close the drawer → Check that the toolbar’s location count equals those locations plus the Location-tool pins shown in this city
  - Expected: After a delete in the List View, the toolbar’s location count drops to match. Known bug: the map keeps its own list, so the count and the marker can stay stale (not confirmed: in run 3 the count had dropped).

- **a refused move reports the error instead of "moved"** — bug · known bug handleLocationDragEnd never checks the PUT status: a refused move still toasts "moved" and the pin snaps back — confirmed run 1,handleLocationDragEnd never checks the PUT status: a refused move still toasts "moved" and the pin snaps back — confirmed run 1 (map.spec.js:2782)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00k). If the group's test pin is missing, creates “[e2e-map] KB pin <time>” through pin mode. Then the test makes the server refuse any location update with an error message.
  - Steps: Click Search, type the pin's name, pick the result, then close its card → Click “Pin New Location” to turn on pin mode → Drag the pin's marker about 60 px right and 90 px down → Click “Move” in “Move Location” → Check that no “'<name>' moved” toast appears → Check that the error message shows as a toast
  - Expected: A refused move shows the server's error instead of “moved”. Known bug: the app ignores the refusal, toasts “'<name>' moved” and the pin snaps back (confirmed on a live run).
  - Web error: Error: no "moved" toast for a refused move

- **the hover-preview Edit asks a viewer to request rights** — suspected · known bug the hover-preview card's Edit is not rights-gated: a viewer gets the edit form, not the permission prompt,the hover-preview card's Edit is not rights-gated: a viewer gets the edit form, not the permission prompt,the hover-preview card's Edit is not rights-gated: a viewer gets the edit form, not the permission prompt (map.spec.js:2850)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00k). If the group's test pin is missing, creates “[e2e-map] KB pin <time>” through pin mode. Then the test makes the app believe the account has no Post right on the Map and reopens the map.
  - Steps: Click Search, type the pin's name, pick the result, then close its card → Hover the pin's marker → Click Edit on the hover preview card → Check that the permission prompt asking to request rights appears → Answer No
  - Expected: For a viewer, Edit on the hover preview asks to request rights. Known bug: this Edit is not rights-checked, so a viewer gets the edit form instead.
  - Web error: Error: expect(locator).toBeVisible() failed

- **"Edit Zone" on the drawn zone asks a viewer to request rights** — suspected · known bug the drawn zone's "Edit Zone" is not rights-gated: a viewer gets the zone form, not the permission prompt,the drawn zone's "Edit Zone" is not rights-gated: a viewer gets the zone form, not the permission prompt,the drawn zone's "Edit Zone" is not rights-gated: a viewer gets the zone form, not the permission prompt (map.spec.js:2870)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00k).
  - Steps: In the Studio Zones drawer create “[e2e-map] KB viewer zone <time>” with “60 mi” → Make it the drawn zone (the test stores it as the city's remembered zone; by hand, click “Map” on its row) → The test makes the app believe the account has no Post right on the Map, then reopens the map → Check that the toolbar reads “Zone: <the zone>” → Click the map inside the zone circle, a little right of the centre → Click “Edit Zone” on the zone card → Check that the permission prompt asking to request rights appears, then answer No
  - Expected: For a viewer, “Edit Zone” on the drawn zone asks to request rights. Known bug: “Edit Zone” is not rights-checked, so a viewer gets the zone form instead.
  - Web error: Error: expect(locator).toBeVisible() failed

### Map — known bugs (correct behaviour asserted)

- **seed: the e2e city exists** — passed (map.spec.js:2670)
  - Where: Tools → Map, with the browser location set near Alice Springs · account: iPhone XRR
  - Setup: Before the group runs: removes any leftover e2e data, then adds a throwaway city (named by Google, e.g. Alice Springs) from the Cities drawer's “Current Location” suggestion at an emulated location near Alice Springs.
  - Steps: Open the Cities drawer → Click Add on the “Current Location” suggestion card → Click “Add City” in the Add City dialog → Check that the city was saved without an error
  - Expected: The throwaway e2e city exists, so the known-bug tests of the group can use it.

- **"Map" on a zone just created in the drawer draws that zone** — passed · known bug the Studio Zones drawer keeps its own list: MapView's zones are stale, so "Map" on a fresh zone draws the old first zone (or none) — NOT reproduced in run 1 (passed): the seeded session processes its own socket echoes, which refresh MapView; verify in a real QR session,the Studio Zones drawer keeps its own list: MapView's zones are stale, so "Map" on a fresh zone draws the old first zone (or none) — NOT reproduced in run 1 (passed): the seeded session processes its own socket echoes, which refresh MapView; verify in a real QR session (map.spec.js:2672)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00k).
  - Steps: Click the Studio Zone button in the toolbar to open the Studio Zones drawer → Click “Add Zone” → Type “[e2e-map] KB fresh zone <time>” in Zone Name and choose “30 mi” → Click “Create Zone” and check that the save succeeds → Click “Map” on the new zone's row → Check that the toolbar's zone button reads “Zone: <the new zone>” within 10 seconds
  - Expected: “Map” on a zone created a moment ago draws that zone, and the toolbar names it. Known bug: the drawer keeps its own list, so the map can draw the old first zone or none (not reproduced on run 1; re-check in a real QR-code session).

- **deleting the drawn zone in the drawer moves the map off it** — passed · known bug a zone deleted in the drawer stays drawn: MapView never re-reads its zones, so the toolbar keeps naming it — NOT reproduced in run 1 (passed; own-device socket echo in the seeded session),a zone deleted in the drawer stays drawn: MapView never re-reads its zones, so the toolbar keeps naming it — NOT reproduced in run 1 (passed; own-device socket echo in the seeded session) (map.spec.js:2684)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00k).
  - Steps: In the Studio Zones drawer create “[e2e-map] KB keep zone <time>” (Add Zone, 30 mi, Create Zone) → Create a second zone “[e2e-map] KB doomed zone <time>” the same way → Make the doomed zone the drawn one and reopen the map (the test stores it as the city's remembered zone; by hand, click “Map” on its row) → Check that the toolbar reads “Zone: <doomed zone>” → Open the Studio Zones drawer and delete the doomed zone (trash button, then Delete in “Delete Studio Zone”) → Close the drawer → Check that the toolbar no longer names the doomed zone
  - Expected: After the drawn zone is deleted in the drawer, the map stops drawing it. Known bug: the map never re-reads its zones, so the toolbar can keep naming the deleted zone (not reproduced on run 1; re-check in a real QR-code session).

- **a zone edit keeps the stored address and description** — bug · known bug PUT /v2/map is a full replacement, but the zone form sends no location_address / description: a zone saved with them (e.g. from mobile) loses them on any web edit — confirmed run 1,PUT /v2/map is a full replacement, but the zone form sends no location_address / description: a zone saved with them (e.g. from mobile) loses them on any web edit — confirmed run 1 (map.spec.js:2706)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00k).
  - Steps: In the Studio Zones drawer create “[e2e-map] KB address zone <time>” (Add Zone, 30 mi, Create Zone) → Click Edit on the new zone's row → In “Edit Studio Zone” choose “40 mi” → Click “Update Zone” → Check that the update sent to the server includes the zone's address and description
  - Expected: A zone edit sends the stored address and description back, so they are kept. Known bug: the zone form sends neither, so any web edit wipes an address or description saved elsewhere (for example from mobile) — confirmed on a live run.
  - Web error: Error: expect(received).toHaveProperty(path)

- **a refused city delete keeps the city selected and the drawer open** — passed · known bug Sidebar.handleConfirmDelete calls selectCity(null) even when the DELETE was refused: the map resets and the drawer closes — NOT reproduced in run 1 (passed),Sidebar.handleConfirmDelete calls selectCity(null) even when the DELETE was refused: the map resets and the drawer closes — NOT reproduced in run 1 (passed) (map.spec.js:2724)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group's throwaway e2e city (see MAP-S00k). The test makes the server refuse any city delete with an error message.
  - Steps: Open the Cities drawer → Click Delete on the e2e city's row → Click Delete in “Delete City” → Check that the error message shows as a toast → Check that the Cities drawer is still open → Check that the toolbar still shows the e2e city
  - Expected: A refused delete leaves the city selected and the Cities drawer open. Known bug: the app deselects the city and closes the drawer even when the delete is refused (not reproduced on run 1).

### Map — two users (realtime, slot b)

- **a city slot b adds appears in A without a reload (and nothing throws)** — passed · known bug legacy Header.jsx city_* listeners throw if the server still emits the un-prefixed city:* events (getCityData is undefined),legacy Header.jsx city_* listeners throw if the server still emits the un-prefixed city:* events (getCityData is undefined) (map.spec.js:2936)
  - Where: Tools → Map in two browsers: iPhone XRR watches; Lava Device's browser location is set near Coober Pedy · account: iPhone XRR and Lava Device
  - Setup: Before the group: removes any leftover e2e data. Lava Device creates the group's city in this test.
  - Steps: iPhone XRR: open Tools → Map and note the number on the floating Cities button → Lava Device: open Tools → Map and open the Cities drawer → Lava Device: click Add on the “Current Location” suggestion, then “Add City” → iPhone XRR: without reloading, check that the Cities number goes up by one within 45 seconds → iPhone XRR: open the Cities drawer and check that Lava Device's new city is listed → iPhone XRR: check that the page raised no script error
  - Expected: The city Lava Device adds appears on iPhone XRR's map live, with no script error. Known bug: older city listeners can throw a script error if the server still sends the old-style city events.

- **a pin slot b adds in A's open city updates A's count, with no unread badge** — passed (map.spec.js:2955)
  - Where: Tools → Map in two browsers, both on the city Lava Device created in MAP-M01 · account: iPhone XRR and Lava Device
  - Setup: Uses the city Lava Device created in MAP-M01.
  - Steps: iPhone XRR: open Tools → Map on that city and wait until the toolbar’s location count shows all of its locations → iPhone XRR: note the number on the red unread badge of the floating Cities button once it stops changing (the sandbox’s other cities carry their own) → Lava Device: reopen the map and select the same city in the Cities drawer → Lava Device: click “Pin New Location”, click the map centre, click “Add Location”, type “[e2e-map] B pin <time>” and click “Save Location” → iPhone XRR: without reloading, check that the location count goes up by one within 45 seconds → iPhone XRR: check that the Cities unread number is unchanged and that the open city’s row in the Cities drawer has no red badge → iPhone XRR: click Search, type the pin’s name, pick it and check that its card opens
  - Expected: Lava Device’s new pin updates iPhone XRR’s count live and can be opened there, with no new unread badge for the city iPhone XRR is already viewing.

- **a zone slot b adds is drawn on A** — passed (map.spec.js:2995)
  - Where: Tools → Map in two browsers, both on the city Lava Device created in MAP-M01 · account: iPhone XRR and Lava Device
  - Setup: Uses the city Lava Device created in MAP-M01.
  - Steps: iPhone XRR: open Tools → Map on that city → Lava Device: reopen the map and select the same city in the Cities drawer → Lava Device: open the Studio Zones drawer, click “Add Zone”, type “[e2e-map] B zone <time>”, choose “30 mi” and click “Create Zone” → iPhone XRR: without reloading, check that the toolbar's zone button reads “Zone: <Lava Device's zone>” within 45 seconds
  - Expected: The zone Lava Device creates is drawn on iPhone XRR's map live.

- **a type slot b adds and deletes shows and goes in A's LOC Types drawer** — passed (map.spec.js:3007)
  - Where: Tools → Map in two browsers; iPhone XRR has the LOC Types drawer open · account: iPhone XRR and Lava Device
  - Setup: Uses the city Lava Device created in MAP-M01.
  - Steps: iPhone XRR: open Tools → Map on that city and click “LOC Types” → Lava Device: reopen the map and click “LOC Types” → Lava Device: click “Add Type”, type “[e2e-map] B type <time>” and click “Create” → iPhone XRR: check that the new type appears in the open drawer within 45 seconds → Lava Device: delete the type (its delete button, then Delete in “Delete Location Type”) → iPhone XRR: check that the type disappears from the drawer within 45 seconds
  - Expected: A type Lava Device adds shows up in iPhone XRR's open LOC Types drawer live, and goes again when Lava Device deletes it.

- **a reorder by slot b re-reads A's city list — needs two adjacent e2e cities and edits the shared order** — skipped (map.spec.js:2951)

### Map — edge: inputs, validation and button states (no writes)

- **whitespace-only name, address and custom type are refused like empty ones @smoke** — passed (map.spec.js:2997)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Steps: Click List View, then “New Location” → Type only spaces in Location Name and click “Save Location” → Check the toast “Please fill in required fields” → Type a real name, type only spaces in the address field and click “Save Location” → Check the toast “Please enter an address or search for a location” → If the project has an “Other” type: pick it, type only spaces in the custom type name, type an address and click “Save Location” → Check the toast “Please specify the custom location type” → Click Cancel and check that nothing was sent to the server
  - Expected: Spaces-only name, address and custom type are refused with the same messages as empty ones, and nothing is saved.

- **the type form: Create / Add disabled on blank, subtypes trimmed and de-duplicated, icon cleared @smoke** — passed (map.spec.js:3029)
  - Where: Tools → Map, LOC Types drawer → Add Type · account: iPhone XRR
  - Steps: Click “LOC Types”, then “Add Type” → Check that the name field has focus, and “Create” is disabled when empty or only spaces and enabled after typing “x” → Check that the subtype “Add” button is disabled while the subtype field is empty or only spaces, and that Enter on spaces adds nothing → Type “  e2e Sub  ” in “Add a subtype...” and press Enter; check one “e2e Sub” chip appears and the field empties → Type “e2e Sub” again and click Add; check there is still one chip and the text stays in the field → Type “E2E SUB” and press Enter; note whether this case variant is accepted → Remove the “e2e Sub” chip with its X and check it is gone → Click “Choose an icon”, pick 🎬, then clear it under “Change icon”; check “Choose an icon” is back → Click Cancel; check the New Type form closes and no type was sent to the server
  - Expected: Create and Add stay disabled on blank input, subtypes are trimmed and exact duplicates ignored, the icon can be cleared, and nothing is saved. Whether a case variant subtype is accepted is only recorded.

- **Street Intersection: Select stays disabled on blank, and Create explains what is missing @smoke** — passed (map.spec.js:3082)
  - Where: Tools → Map, Studio Zones drawer → Add Zone · account: iPhone XRR
  - Steps: Click the Studio Zone button in the toolbar, then “Add Zone” → Click “Street Intersection” → Check that “Select Intersection” is disabled with Street 1 empty and with only spaces in it → Empty Street 1 and click “Create Zone”; check the toast “Please enter street 1” → Type “e2e street” in Street 1; check that “Select Intersection” becomes enabled → Click “Create Zone”; check the toast asking to select the intersection first by clicking “Select Intersection” → Click Cancel and check that no zone was sent to the server
  - Expected: Select Intersection stays disabled until Street 1 has text, and Create Zone explains what is missing instead of saving.

- **Custom radius: negative, below 1, decimal and over 200 never submit @smoke** — passed (map.spec.js:3112)
  - Where: Tools → Map, Studio Zones drawer → Add Zone → Custom radius · account: iPhone XRR
  - Steps: Open the Studio Zones drawer, click “Add Zone”, then “Custom” → Type -5 in the radius field and leave it; check it is flagged too low with “Enter a radius greater than 0 miles.”; click “Create Zone” → Type 0; check the same flag and message; click “Create Zone” → Type 0.5; check the browser flags it too low but no message shows; click “Create Zone” → Type 1.5; check the browser flags it as not a whole number, with no message; click “Create Zone” → Type 201; check the browser flags it too high, with no message; click “Create Zone” → Check that no zone was sent to the server → Click Cancel
  - Expected: None of the values is submitted: −5 and 0 show the form's message, while 0.5, 1.5 and 201 are blocked silently by the browser's own checks (recorded as a quirk).

- **Custom radius 1, 200 and 1e2 are posted as 1, 200 and 100 miles (stubbed)** — passed (map.spec.js:3145)
  - Where: Tools → Map, Studio Zones drawer → Add Zone → Custom radius · account: iPhone XRR
  - Setup: The test answers every zone save itself with success, so no zone is really created.
  - Steps: Open the Studio Zones drawer, click “Add Zone”, then “Custom” → Type 1 in the radius field and click “Create Zone” → Check that the save asks for 1 mile and the form closes → Repeat with 200 and check that the save asks for 200 miles → Repeat with 1e2 and check that the save asks for 100 miles
  - Expected: The radii 1, 200 and 1e2 are accepted and sent as 1, 200 and 100 miles.

- **Add City stays disabled while the search text was typed but nothing picked @smoke** — passed (map.spec.js:3168)
  - Where: Tools → Map, Cities drawer → Add Another City · account: iPhone XRR
  - Steps: Open the Cities drawer and click “Add Another City” → Check that the dialog's “Add City” button is disabled → Type “zz-e2e typed only” in “Search for a city...” without picking a suggestion → Check that “Add City” is still disabled and the auto-filled coordinates are empty → Click Cancel and check that no city was sent to the server
  - Expected: Typed text alone never enables Add City: a place must be picked from the suggestions first.

- **Update Location shows "Saving..." and a double click sends one PUT (stubbed)** — passed (map.spec.js:3291)
  - Where: Tools → Map, any city that is open on arrival → List View · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub 0” to the city's location list (nothing is saved) and answers any location update itself, after 3 seconds.
  - Steps: Click List View, then Edit on the “[e2e-map] stub 0” card → Type “edited” in the description → Double-click “Update Location” → Check that the button shows “Saving...” and is disabled → Check that the form closes once the answer arrives → Check that only one update was sent
  - Expected: While saving, the button reads “Saving...” and is disabled, so a double click sends a single update.

- **Create Zone shows "Saving..." and a double click sends one POST (stubbed)** — passed (map.spec.js:3204)
  - Where: Tools → Map, Studio Zones drawer → Add Zone · account: iPhone XRR
  - Setup: The test answers any zone save itself, after 3 seconds, so no zone is really created.
  - Steps: Open the Studio Zones drawer and click “Add Zone” → Double-click “Create Zone” with the default name and radius → Check that the button shows “Saving...” → Check that the form closes once the answer arrives → Check that only one save was sent
  - Expected: While saving, the button reads “Saving...”, so a double click sends a single zone save.

- **a pending Delete confirm locks Cancel, the close button and Escape (stubbed)** — passed (map.spec.js:3221)
  - Where: Tools → Map, any city that is open on arrival → List View · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub 0” to the city's location list (nothing is saved) and answers its delete itself, after 4 seconds.
  - Steps: Click List View, then the trash button on the “[e2e-map] stub 0” card → Click Delete in “Delete Location” → Check that Cancel is disabled and the dialog has no close (X) button → Press Escape and check that the dialog stays open → Check that the dialog closes once the answer arrives
  - Expected: While the delete is pending, the dialog cannot be cancelled or dismissed, and it closes when the server answers.

- **more than eight matches: eight results and "+N more results" @smoke** — passed (map.spec.js:3237)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test adds ten fake locations, “[e2e-map] stub 0” to “[e2e-map] stub 9”, to the city's location list (nothing is saved).
  - Steps: Click Search → Type “[e2e-map] stub” → Check that eight results are listed → Check that “+2 more results” is shown
  - Expected: With ten matches the search lists eight results and says “+2 more results”.

- **regex-like and unicode queries never break the search @smoke** — passed (map.spec.js:3245)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] Ünïcödé 日本 مرحبا 🎬” to the city's location list (nothing is saved).
  - Steps: Click Search → Type each of these in turn: .*  (  [  \  %  $^  😀 → After each, check that results or “No locations found” are shown → Type “ünïcödé 日本” in lower case → Check that exactly one result containing “Ünïcödé” is listed → Check that no “Something went wrong” page shows
  - Expected: Special characters never break the search, and a lower-case unicode query finds the upper-case unicode name.

- **Clear All appears only with a filter; Search and Filter close each other and clear the query @smoke** — passed (map.spec.js:3257)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test adds two fake locations of types “Edge A” and “Edge B” to the city's location list (nothing is saved).
  - Steps: Click Filter and check that there is no “Clear All” → Click the “Edge A” chip; check that “Clear All” appears and the header count reads “1 / N locations” → Click Search; check that the filter panel closes and the Filter button reads “Filter (1)” → Type “zz”, close Search and open it again; check that the search box is empty → Type “zz” and click Filter; check that the search box closes → Click Search again and check that the search box is empty
  - Expected: Clear All shows only while a type filter is on; opening Search closes Filter and vice versa, and closing Search clears the query.

- **HTML, script, unicode, RTL and very long names render as plain text @smoke** — passed (map.spec.js:3284)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test adds four fake locations to the city's location list (nothing is saved): one whose name and description contain HTML and script code, one with a unicode name, one with an Arabic (right-to-left) name, and one with a 1,000-character name.
  - Steps: Search for the HTML-named location and open it → Check that its card shows the name and description as literal text, with no bold text or image → Search for and open the unicode and the Arabic location; check each card shows its name exactly → Search for and open the long-named location; check the card stays 320 px wide and the page does not scroll sideways → Close the card and click List View; check that the HTML card shows no bold text or image → Click View on the HTML card; check that the detail drawer shows the description as text, without italics → Check that none of the injected script ran
  - Expected: HTML and script in names and descriptions show as plain text and never run; unicode and right-to-left names display correctly; a very long name does not widen the card or the screen.

- **poles, the antimeridian and missing / non-numeric coordinates do not break the map @smoke** — passed (map.spec.js:3314)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test adds four fake locations to the city's location list (nothing is saved): “[e2e-map] north pole” at 90, 180, “[e2e-map] south pole” at −90, −180, “[e2e-map] bad coords” with non-numeric coordinates, and “[e2e-map] no coords” with none.
  - Steps: Check that the toolbar count includes every location of the city, the four fakes too, plus its Location-tool pins → Search for “[e2e-map] north pole” and open it; check its card shows “90.000000, 180.000000”; close it → Search for “[e2e-map] bad coords” and click the result; check that the search stays open and no card opens → Close Search and click List View; check that the “[e2e-map] no coords” card shows no coordinates; close the drawer → Click “Fit All” → Check that no “Something went wrong” page shows
  - Expected: Pole, date-line, broken and missing coordinates never break the map: they are counted, the pole opens with its coordinates, the broken one opens nothing, and Fit All still works.

- **a pin exactly on the prime meridian (longitude 0) is drawn and opens** — bug · known bug coordinates are read with `a || b` fallbacks (locationHelpers, MapView): a latitude or longitude of exactly 0 counts as missing, so a pin on the equator or the prime meridian is never drawn or opened — confirmed run 3,coordinates are read with `a || b` fallbacks (locationHelpers, MapView): a latitude or longitude of exactly 0 counts as missing, so a pin on the equator or the prime meridian is never drawn or opened — confirmed run 3 (map.spec.js:3448)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] greenwich” at latitude 51.4779, longitude 0 to the city's location list (nothing is saved).
  - Steps: Click Search and type “[e2e-map] greenwich” → Check that the result is listed and pick it → Check that its card opens → Check that the card shows “51.477900, 0.000000”
  - Expected: A pin at longitude 0 is drawn and its card opens with its coordinates. Known bug: a latitude or longitude of exactly 0 is read as missing, so the result is listed but no card opens and no pin is drawn.
  - Web error: Error: expect(locator).toBeVisible() failed

- **an out-of-range lat / lng in the form URL is not submitted (Classic, stubbed)** — bug · known bug the full-page form posts whatever lat / lng the URL carries (parseFloat, no range check): 91 / 181 go out as is, "abc" / "xyz" as null — confirmed run 3,the full-page form posts whatever lat / lng the URL carries (parseFloat, no range check): 91 / 181 go out as is, "abc" / "xyz" as null — confirmed run 3 (map.spec.js:3468)
  - Where: Classic view (sidebar “Classic view”) → the Map's full-page New Location form, opened by a direct link carrying latitude 91 and longitude 181 · account: iPhone XRR
  - Setup: The test answers any location save itself, so nothing is written even if the form sends one.
  - Steps: Switch to Classic view and open the New Location form with latitude 91 and longitude 181 in the link → Type “[e2e-map] out-of-range coords <time>” in Location Name → Type “[e2e-map] typed” in the address field → Click “Save Location” → Wait 3 seconds and check that no save was sent
  - Expected: Out-of-range coordinates from the link are refused and nothing is sent. Known bug: the form posts whatever latitude and longitude the link carries (91 and 181 here).
  - Web error: Error: expect(received).toHaveLength(expected)

- **an non-numeric lat / lng in the form URL is not submitted (Classic, stubbed)** — bug · known bug the full-page form posts whatever lat / lng the URL carries (parseFloat, no range check): 91 / 181 go out as is, "abc" / "xyz" as null — confirmed run 3,the full-page form posts whatever lat / lng the URL carries (parseFloat, no range check): 91 / 181 go out as is, "abc" / "xyz" as null — confirmed run 3 (map.spec.js:3468)
  - Where: Classic view (sidebar “Classic view”) → the Map's full-page New Location form, opened by a direct link carrying latitude “abc” and longitude “xyz” · account: iPhone XRR
  - Setup: The test answers any location save itself, so nothing is written even if the form sends one.
  - Steps: Switch to Classic view and open the New Location form with “abc” and “xyz” as latitude and longitude in the link → Type “[e2e-map] non-numeric coords <time>” in Location Name → Type “[e2e-map] typed” in the address field → Click “Save Location” → Wait 3 seconds and check that no save was sent
  - Expected: Non-numeric coordinates from the link are refused and nothing is sent. Known bug: the form posts them anyway, as empty coordinates.
  - Web error: Error: expect(received).toHaveLength(expected)

### Map — edge: failed, slow and empty reads, and recovery (stubs)

- **cities read aborted (network error) is reported** — passed (map.spec.js:3505)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: Before the map opens, the test makes the read of the city list fail as if the network dropped.
  - Steps: Open Tools → Map → Wait for the map to show its no-city state with the floating “Add City” button → Check that an error toast appears within 60 seconds → Check that no “Something went wrong” page shows
  - Expected: A city list that fails on the network is reported with an error toast, and the map does not crash.

- **locations read aborted (network error) is reported** — passed (map.spec.js:3505)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: Before the map opens, the test makes the read of the city's locations fail as if the network dropped.
  - Steps: Open Tools → Map → Wait for a city to open → Check that an error toast appears within 60 seconds → Check that no “Something went wrong” page shows
  - Expected: A location list that fails on the network is reported with an error toast, and the map does not crash.

- **locations read HTTP 500 without an envelope is reported** — bug · known bug isApiFailure is status === 0 only, and the interceptor turns an HTTP 500 without an envelope into { error, httpStatus: 500 }: the read becomes an empty list, with no error — confirmed run 3,isApiFailure is status === 0 only, and the interceptor turns an HTTP 500 without an envelope into { error, httpStatus: 500 }: the read becomes an empty list, with no error — confirmed run 3 (map.spec.js:3505)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: Before the map opens, the test makes the read of the city's locations fail with a server error that has no message.
  - Steps: Open Tools → Map → Wait for a city to open → Check that an error toast appears within 60 seconds → Check that no “Something went wrong” page shows
  - Expected: A server error on the location list is reported with an error toast, and the map does not crash. Known bug: a server error without the usual reply reads as an empty list, with no message (seen in every run).
  - Web error: Error: an error toast

- **studio zones read aborted (network error) is reported** — passed (map.spec.js:3505)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: Before the map opens, the test makes the read of the city's studio zones fail as if the network dropped.
  - Steps: Open Tools → Map → Wait for a city to open → Check that an error toast appears within 60 seconds → Check that no “Something went wrong” page shows
  - Expected: A studio-zone list that fails on the network is reported with an error toast, and the map does not crash.

- **studio zones read HTTP 500 without an envelope is reported** — bug · known bug isApiFailure is status === 0 only, and the interceptor turns an HTTP 500 without an envelope into { error, httpStatus: 500 }: the read becomes an empty list, with no error — confirmed run 3,isApiFailure is status === 0 only, and the interceptor turns an HTTP 500 without an envelope into { error, httpStatus: 500 }: the read becomes an empty list, with no error — confirmed run 3 (map.spec.js:3505)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: Before the map opens, the test makes the read of the city's studio zones fail with a server error that has no message.
  - Steps: Open Tools → Map → Wait for a city to open → Check that an error toast appears within 60 seconds → Check that no “Something went wrong” page shows
  - Expected: A server error on the studio-zone list is reported with an error toast, and the map does not crash. Known bug: a server error without the usual reply reads as an empty list, with no message (seen in every run).
  - Web error: Error: an error toast

- **location types read aborted (network error) is reported** — passed (map.spec.js:3505)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: Before the map opens, the test makes the read of the location types fail as if the network dropped.
  - Steps: Open Tools → Map → Wait for the map to be ready → Check that an error toast appears within 60 seconds → Check that no “Something went wrong” page shows
  - Expected: A location-type list that fails on the network is reported with an error toast, and the map does not crash.

- **location types read HTTP 500 without an envelope is reported** — bug · known bug isApiFailure is status === 0 only, and the interceptor turns an HTTP 500 without an envelope into { error, httpStatus: 500 }: the read becomes an empty list, with no error — confirmed run 3,isApiFailure is status === 0 only, and the interceptor turns an HTTP 500 without an envelope into { error, httpStatus: 500 }: the read becomes an empty list, with no error — confirmed run 3 (map.spec.js:3505)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: Before the map opens, the test makes the read of the location types fail with a server error that has no message.
  - Steps: Open Tools → Map → Wait for the map to be ready → Check that an error toast appears within 60 seconds → Check that no “Something went wrong” page shows
  - Expected: A server error on the location types is reported with an error toast, and the map does not crash. Known bug: a server error without the usual reply reads as an empty list, with no message (seen in every run).
  - Web error: Error: an error toast

- **pins a failing Location-tool pin read is silent and shows no pins @smoke** — passed (map.spec.js:3521)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: Before the map opens, the test makes the server refuse the read of the Location-tool pins with an error message.
  - Steps: Open Tools → Map → Check that the toolbar's location count equals only the city's own map locations, with no Location-tool pins → Wait 2 seconds → Check that no error toast appears
  - Expected: A refused Location-tool pin read is silent: no message is shown and the pins are simply left out (recorded as an observation).

- **the List View shows the error, then recovers when reopened @smoke** — passed (map.spec.js:3421)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Steps: Open Tools → Map and wait for the city's locations to load → The test now makes the server refuse the city's location list with an error message → Click List View → Check that the error shows as a toast and the drawer says “No locations yet” → Close the drawer → The test lets the location list through again → Click List View again → Check that the drawer lists one card per location the server returns (or “No locations yet” if there are none)
  - Expected: A refused read shows the error and an empty list, the same as a city with no locations and with no retry button (recorded); reopening the drawer reads again and recovers.

- **the Studio Zones drawer shows the error, then recovers when reopened @smoke** — passed (map.spec.js:3439)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Steps: Open Tools → Map and wait for the city's locations to load → The test now makes the server refuse the city's studio-zone list with an error message → Open the Studio Zones drawer → Check that the error shows as a toast and the drawer says “No studio zones yet” → Close the drawer → The test lets the zone list through again → Open the Studio Zones drawer again → Check that it lists one row per zone the server returns → If there are zones, check that the toolbar's zone button reads “Zone: …” again
  - Expected: A refused zone read shows the error and the empty state; reopening the drawer reads again and recovers the list and the drawn zone.

- **failed type and city reads come back only after the map is reopened @smoke** — passed (map.spec.js:3456)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: Before the map opens, the test makes the server refuse the location-type list with an error message.
  - Steps: Open Tools → Map → Click “LOC Types” and check that it says “No location types yet” → The test lets the type list through again; close the drawer → Reopen the map → Click “LOC Types” → Check that the drawer's count reads “<n> Types”, matching the types the server returns
  - Expected: After a refused type read the drawer says “No location types yet” and never re-reads by itself; the types come back only after the map is reopened (recorded as an observation). Only the type read is failed, not the city read the title mentions.

- **offline: the List View read fails with a message and works again online @smoke** — passed (map.spec.js:3471)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Steps: Open Tools → Map → Take the browser offline → Click List View and check that an error toast appears within 30 seconds → Close the drawer → Bring the browser back online → Click List View again → Check that “Loading locations...” goes away and no “Something went wrong” page shows
  - Expected: Offline, the List View read fails with a message; back online, it reads and shows the list again.

- **slow cities: the floating button says "Add City" meanwhile, and its drawer is closed by the arriving city @smoke** — passed (map.spec.js:3488)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: The test holds back the city list for 8 seconds.
  - Steps: Open Tools → Map → If the floating “Add City” button shows while the cities load, click it → Check that the Cities drawer opens with a loading spinner → Wait for the city list to arrive → If the project has cities, check that the drawer closes by itself when a city opens automatically → Check that the map ends up showing a city (or the no-city state)
  - Expected: While cities load, the map offers “Add City” as if the project had none, and the city that then opens automatically closes the drawer the user had opened; both are recorded as observations, not failures.

- **slow types: the LOC Types drawer spins, then lists @smoke** — passed (map.spec.js:3513)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test holds back the location-type list for 8 seconds.
  - Steps: Open Tools → Map → Click “LOC Types” → Note whether the drawer shows its loading spinner → Check that the spinner goes away within 30 seconds and the types are listed
  - Expected: The LOC Types drawer shows a spinner while the types load, then lists them.

- **no location types: the empty state and "Add First Type" opens the form @smoke** — passed (map.spec.js:3530)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test makes the location-type list come back empty.
  - Steps: Open Tools → Map and click “LOC Types” → Check that it says “No location types yet” and “0 Types” → Check that there is no “Filter by type or subtype...” box → With the Map Post right: click “Add First Type” and check that the “New Type” form opens → Click Cancel
  - Expected: With no types the drawer shows its empty state, and “Add First Type” opens the New Type form.

- **no zones: the toolbar says "Studio Zone" and the drawer shows its empty state @smoke** — passed (map.spec.js:3544)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test makes the studio-zone list come back empty.
  - Steps: Open Tools → Map → Check that the toolbar's zone button reads just “Studio Zone” → Open the Studio Zones drawer → Check that it says “No studio zones yet” → Check that there is no “Filter by zone name or address...” box
  - Expected: With no zones the toolbar says “Studio Zone” and the drawer shows its empty state without a filter box.

- **a stubbed zone is drawn, listed and detailed (no address row when it has none) @smoke** — passed (map.spec.js:3554)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test makes the studio-zone list return one fake zone, “[e2e-map] stub zone”, 7 miles around the city centre and with no address (nothing is saved).
  - Steps: Open Tools → Map → Check that the toolbar's zone button reads “Zone: [e2e-map] stub zone” → Open the Studio Zones drawer → Check that the zone's row shows “7 mi radius” and “Active” → Click “Details” on the row → Check that the details show “Radius” and no “Address” row
  - Expected: The zone is drawn and named in the toolbar, listed as active with its radius, and its details leave out the Address row because it has none.

- **a location with no address, description or photos shows only what it has @smoke** — passed (map.spec.js:3580)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] bare” with no address, description or photos to the city's location list (nothing is saved).
  - Steps: Click List View → Click View on the “[e2e-map] bare” card → Check that the detail drawer shows “Coordinates” → Check that it shows no Address, Description, Subtypes or Photos section
  - Expected: The detail drawer shows only the sections the location has: Coordinates, and nothing for the missing address, description, subtypes or photos.

- **no types to filter: "No types available" @smoke** — passed (map.spec.js:3592)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test makes the city's location list and the Location-tool pins come back empty.
  - Steps: Open Tools → Map → Click Filter → Check that the filter panel says “No types available”
  - Expected: With no locations to group by type, the filter panel says “No types available”.

### Map — edge: Classic view read views

- **Classic: the map is a full page at /film-tools/map with the same toolbar @smoke** — passed (map.spec.js:3725)
  - Where: Classic view (sidebar “Classic view”) → Tools → Map · account: iPhone XRR
  - Steps: Switch to Classic view → Open Tools → Map → Check that the map fills the page on its own Map page, with no tool window around it → Check that “How To Use Map” is shown → When a city is open, check that Search, Filter, List View, the Studio Zone button and “LOC Types” are shown
  - Expected: In Classic view the map is a full page with the same toolbar as in Windowed view.

- **Classic: every drawer opens and closes @smoke** — passed (map.spec.js:3621)
  - Where: Classic view (sidebar “Classic view”) → Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Steps: Switch to Classic view and open Tools → Map → Open the Cities drawer and close it → Open List View and close it → Open the Studio Zones drawer and close it → Open “LOC Types” and close it → Check that no “Something went wrong” page shows
  - Expected: In Classic view every drawer of the map opens and closes without an error.

- **Classic: "View" changes the URL to the Locations page; "Map View" comes back @smoke** — passed (map.spec.js:3631)
  - Where: Classic view (sidebar “Classic view”) → Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub pin” to the city's location list (nothing is saved).
  - Steps: Switch to Classic view, open Tools → Map and note the open city → Click Search, type “[e2e-map] stub pin” and open its card → Click “View” on the card → Check that the browser moves to the Locations page (heading “Locations”) → Click “Map View” → Check that the browser is back on the map with the same city open
  - Expected: In Classic view, “View” moves the page to the Locations page and “Map View” brings back the map on the same city.

- **Classic: a reload keeps the route and the city @smoke** — passed (map.spec.js:3758)
  - Where: Classic view (sidebar “Classic view”) → Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Steps: Switch to Classic view, open Tools → Map and note the open city → Reload the page → Check that the browser stays on the Map page → Check that the same city opens again
  - Expected: In Classic view a reload keeps the Map page and the city that was open.

- **Classic: the theme toggle works and is restored @smoke** — passed (map.spec.js:3655)
  - Where: Classic view (sidebar “Classic view”) → Tools → Map · account: iPhone XRR
  - Steps: Switch to Classic view and open Tools → Map → Note whether the map is light or dark → Click the theme switch (“Switch to dark mode” or “Switch to light mode”) → Check that the map's theme changed → Click the theme switch again → Check that the original theme is back
  - Expected: In Classic view the theme switch flips the map's theme and a second click restores it.

### Map — edge: light theme on every drawer and modal

- **MAP-ET-light drawers, modals and cards paint light, and List View tabs stay legible @smoke** — passed (map.spec.js:3668)
  - Where: Tools → Map with the app theme set to light, any city that is open on arrival · account: iPhone XRR
  - Setup: The test sets the app theme to light before the map loads and adds two fake locations, “[e2e-map] stub 0” (type “Edge A”) and “[e2e-map] stub 1” (type “Edge B”), to the city's location list (nothing is saved).
  - Steps: Open Tools → Map and check that it shows the light theme → Open the Cities drawer, check that it paints light, and close it → Click List View; check that it paints light and the inactive “Edge B” tab's text keeps at least 3:1 contrast → Click View on “[e2e-map] stub 0”; check that the detail drawer paints light; close it → With the Map Post right: open the card's Delete confirm, check it paints light, Cancel; click “New Location”, check the form paints light, Cancel → Open the Studio Zones drawer and check it paints light; with the Post right click “Add Zone”, check the zone form paints light, Cancel → Click “LOC Types”, check that it paints light, and close it → With the Post right: Cities drawer → “Add Another City”; check the Add City dialog paints light; Cancel → Search for “[e2e-map] stub 0”, open it and check that its card paints light
  - Expected: Every drawer, dialog and card of the map paints a light background in the light theme, and the List View type tabs stay readable.

### Map — edge: dark theme on every drawer and modal

- **MAP-ET-dark drawers, modals and cards paint dark, and List View tabs stay legible @smoke** — passed (map.spec.js:3668)
  - Where: Tools → Map with the app theme set to dark, any city that is open on arrival · account: iPhone XRR
  - Setup: The test sets the app theme to dark before the map loads and adds two fake locations, “[e2e-map] stub 0” (type “Edge A”) and “[e2e-map] stub 1” (type “Edge B”), to the city's location list (nothing is saved).
  - Steps: Open Tools → Map and check that it shows the dark theme → Open the Cities drawer, check that it paints dark, and close it → Click List View; check that it paints dark and the inactive “Edge B” tab's text keeps at least 3:1 contrast → Click View on “[e2e-map] stub 0”; check that the detail drawer paints dark; close it → With the Map Post right: open the card's Delete confirm, check it paints dark, Cancel; click “New Location”, check the form paints dark, Cancel → Open the Studio Zones drawer and check it paints dark; with the Post right click “Add Zone”, check the zone form paints dark, Cancel → Click “LOC Types”, check that it paints dark, and close it → With the Post right: Cities drawer → “Add Another City”; check the Add City dialog paints dark; Cancel → Search for “[e2e-map] stub 0”, open it and check that its card paints dark
  - Expected: Every drawer, dialog and card of the map paints a dark background in the dark theme, and the List View type tabs stay readable.

### Map — edge: narrow window 1024px

- **the toolbar scrolls on its own, the map never shifts, drawers fit @smoke** — passed (map.spec.js:3738)
  - Where: Tools → Map in a 1024 × 768 browser window, any city that is open on arrival · account: iPhone XRR
  - Steps: Open Tools → Map in a window 1024 px wide → Check that the toolbar scrolls sideways on its own, and note whether its buttons fit → Click “Search places on map” and put the cursor in “Search any place...” → Check that the map area itself has not shifted sideways → Scroll the toolbar to the theme switch, click it and check that the theme changes; click it again → Click “LOC Types” and check that the drawer is no wider than the window; close it → Check that the page has no sideways scroll
  - Expected: At 1024 px the toolbar scrolls by itself, the map never shifts sideways, the theme switch stays reachable, and the drawer fits the window.

### Map — edge: narrow window 760px

- **the toolbar scrolls on its own, the map never shifts, drawers fit @smoke** — passed (map.spec.js:3738)
  - Where: Tools → Map in a 760 × 820 browser window, any city that is open on arrival · account: iPhone XRR
  - Steps: Open Tools → Map in a window 760 px wide → Check that the toolbar scrolls sideways on its own, and note whether its buttons fit → Click “Search places on map” and put the cursor in “Search any place...” → Check that the map area itself has not shifted sideways → Scroll the toolbar to the theme switch, click it and check that the theme changes; click it again → Click “LOC Types” and check that the drawer is no wider than the window; close it → Check that the page has no sideways scroll
  - Expected: At 760 px the toolbar scrolls by itself, the map never shifts sideways, the theme switch stays reachable, and the drawer fits the window.

### Map — edge: what survives a reopen

- **the guide is dismissed per tab session: a new session shows it again @smoke** — passed (map.spec.js:3764)
  - Where: Tools → Map, in a fresh tab so the map guide shows · account: iPhone XRR
  - Steps: Open Tools → Map with the “How to Use the Map Module” guide showing → Click the X that dismisses the map guide → Check that the guide is hidden → Open Tools → Map in a brand-new browser session → Check that the guide shows again there
  - Expected: Dismissing the guide lasts only for that tab session; a new session shows it again.

- **filters, search, pin mode and the List View tab reset on reopen @smoke** — passed (map.spec.js:3775)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Setup: The test adds two fake locations, “[e2e-map] stub 0” (type “Edge A”) and “[e2e-map] stub 1” (type “Edge B”), to the city's location list (nothing is saved).
  - Steps: Click Filter, click the “Edge A” chip, and check that the button reads “Filter (1)” → With the Map Post right: click “Pin New Location” and check that pin mode is on (“Cancel Pin Mode”) → Click List View, click the “Edge B” tab, and check “Showing 1 of …”; close the drawer → Reopen the map → Check that the Filter button reads plain “Filter”, pin mode is off and the search box is closed → Click List View and check that it shows its default view (“Browse, search and manage every pinned location”)
  - Expected: After a reopen the type filter, pin mode, search and the List View tab are all back to their defaults.

- **a remembered city that no longer exists falls back to the first city @smoke** — passed (map.spec.js:3911)
  - Where: Tools → Map · account: iPhone XRR
  - Setup: Before the map loads, the test stores a last-visited city that does not exist.
  - Steps: Open Tools → Map → Wait for the city list to load → Check that the first city of the list opens
  - Expected: When the remembered city no longer exists, the map falls back to the first city.

- **a remembered zone that no longer exists falls back to the first zone @smoke** — passed (map.spec.js:3807)
  - Where: Tools → Map, on a city that has studio zones · account: iPhone XRR
  - Steps: Open Tools → Map and wait for the city's zones to load → The test replaces the city's remembered zone with one that does not exist → Reopen the map → Check that the toolbar's zone button reads “Zone: <the city's first zone>”
  - Expected: When the remembered zone no longer exists, the map draws the city's first zone.

- **a pending pin left in sessionStorage is not resumed after a reload @smoke** — passed (map.spec.js:3819)
  - Where: Tools → Map, any city that is open on arrival · account: iPhone XRR
  - Steps: Open Tools → Map → The test leaves a half-finished pin in the tab's storage, as if a pin flow had been interrupted → Reopen the map → Wait 3 seconds → Check that neither the “Add City” dialog nor the “New Location” form opens
  - Expected: A leftover pending pin is not resumed after a reload; whether the stored pin is still there is only recorded.

### Map — edge: saved inputs on e2e data

- **seed: the e2e city and two base locations exist** — passed (map.spec.js:4019)
  - Where: Tools → Map with the browser location set near Alice Springs, then Classic view → the Map's full-page New Location form · account: iPhone XRR
  - Setup: Before the group: removes leftover e2e data, adds the throwaway e2e city (named by Google, e.g. Alice Springs) from the Cities drawer's “Current Location” suggestion, then saves two locations, “[e2e-map] Edge base one <time>” and “[e2e-map] Edge base two <time>”, through the Classic full-page New Location form, just north of the city.
  - Steps: Open the Cities drawer, click Add on the “Current Location” suggestion, then “Add City” → Switch to Classic view and open the full-page New Location form with coordinates just north of the city in the link → Save “[e2e-map] Edge base one <time>”, then do the same for “[e2e-map] Edge base two <time>” → Check that the city and both locations were saved without an error
  - Expected: The e2e city and its two base locations exist, so the other tests of the group can use them.

- **leading and trailing spaces are trimmed from a location name** — passed (map.spec.js:4024)
  - Where: Tools → Map, open the e2e city → List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click List View, then Edit on the first base location's card → Replace Location Name with “   [e2e-map] Edge trimmed <time>   ” (spaces before and after) → Click “Update Location” → Check that the save succeeds and sends the name without the spaces → Check that the List View shows the card under the trimmed name
  - Expected: Leading and trailing spaces are removed from a location name before it is saved and shown.

- **a unicode / emoji / RTL name saves, renders and is found case-insensitively** — passed (map.spec.js:4035)
  - Where: Tools → Map, open the e2e city → List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click List View, then Edit on the first base location's card → Replace the name with “[e2e-map] Ünïcödé 日本 مرحبا 🎬 <time>” and click “Update Location” → Check that the save succeeds and the List View shows the card under the new name → Close the drawer → Click Search and type “ÜNÏCÖDÉ 日本” in capitals → Check that exactly one result with the new name is listed
  - Expected: A name with accented letters, Japanese, Arabic and an emoji saves, displays, and is found by a search in different letter case.

- **an HTML / script name and description are stored and shown as text, never run** — passed (map.spec.js:4049)
  - Where: Tools → Map, open the e2e city → List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click List View, then Edit on the first base location's card → Replace the name with text containing HTML bold and image tags, and the description with text containing a script tag and italics → Click “Update Location” and check that the save succeeds → Check that the card's title shows the name as literal text, with no bold text or image → Click View and check that the detail drawer shows the description as literal text; close it and the List View → Search for the name, open its map card, and check that its title shows the name as literal text → Check that none of the injected script ran
  - Expected: HTML and script typed into a name and description are saved and always shown as plain text, in the card, the detail drawer and the map card; no script runs.

- **a 1,000-character name: the server verdict is recorded and the page does not overflow** — passed (map.spec.js:4071)
  - Where: Tools → Map, open the e2e city → List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click List View, then Edit on the second base location's card → Replace the name with “[e2e-map]” followed by the word “word” 200 times (about 1,000 characters) → Click “Update Location” and note the server's answer → If it saved: check that the form closes, the card's title is cut to at most two lines, and the page has no sideways scroll → If it saved: rename the location back to its old name → If it was refused: check that an error toast shows, then click Cancel
  - Expected: The form has no length limit and the server's answer is only recorded; if the long name saves, the card clamps it to two lines and the page does not overflow.

- **a multi-line description keeps its lines; a whitespace-only one saves empty** — passed (map.spec.js:4093)
  - Where: Tools → Map, open the e2e city → List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click List View, then Edit on the second base location's card → Type “e2e line one” and “e2e line two” on two lines in the description and click “Update Location” → Check that the save sends both lines → Click View on the card and check that the detail drawer shows the two lines; close it and the List View → Edit the same location again, type only spaces as the description and click “Update Location” → Check that the save sends an empty description
  - Expected: A multi-line description keeps its line breaks, and a description of only spaces is saved as empty.

- **a scene number is saved trimmed** — bug · known bug useLocationForm trims name and description but sends scene_number as typed: "  12B  " is stored with its spaces — confirmed run 3,useLocationForm trims name and description but sends scene_number as typed: "  12B  " is stored with its spaces — confirmed run 3 (map.spec.js:4112)
  - Where: Tools → Map, open the e2e city → LOC Types and List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click “LOC Types”, “Add Type”, type “[e2e-map] Edge Shoot <time>” and click Create; check it saves; close the drawer → Click List View, then Edit on the second base location's card → Pick the new type in the type picker → Type “  12B  ” (spaces around) in Scene Number → Click “Update Location” and check that the save succeeds → Check that the save sends the scene number as “12B”
  - Expected: The scene number is saved without its surrounding spaces. Known bug: name and description are trimmed but the scene number is sent as typed, spaces included.
  - Web error: Error: expect(received).toBe(expected) // Object.is equality

- **two locations may share a name: both are listed and both are found** — passed (map.spec.js:4129)
  - Where: Tools → Map, open the e2e city → List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click List View, then Edit on the second base location's card → Give it exactly the first base location's name and click “Update Location” → Note whether the server accepts the duplicate name → If accepted: check that the List View shows two cards with that name; close it → Click Search, type the shared name and check that two results are listed; close Search → Open List View, Edit the second card with that name, rename it “[e2e-map] Edge base two <time>” and check that the save succeeds
  - Expected: Whether two locations may share a name is recorded; when they may, both are listed and both are found by search.

- **a type name is trimmed; a case variant of an existing type is recorded** — passed (map.spec.js:4152)
  - Where: Tools → Map, open the e2e city → LOC Types · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click “LOC Types”, then “Add Type” → Type “   [e2e-map] Edge Case Type <time>   ” (spaces around) and click Create → Check that the save succeeds, sends the name without the spaces, and the trimmed type is listed → Click “Add Type” again, type the same name in capitals and click Create → Note whether the server accepts the case variant; if it is refused, click Cancel → Delete each of the two types that exists
  - Expected: A type name is saved without its surrounding spaces; whether a capitals-only variant of an existing type is accepted is recorded.

- **an HTML / unicode type name renders as text in the drawer, the picker, the chip and the card** — passed (map.spec.js:4172)
  - Where: Tools → Map, open the e2e city → LOC Types, List View and Filter · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click “LOC Types”, “Add Type”, type “[e2e-map] <i>it</i> ✨ مرحبا <time>” and click Create; check that it saves → Check that the type's row shows the name as text, without italics; close the drawer → Click List View, Edit the first base location, pick the new type in the type picker and click “Update Location”; check it saves → Check that the location's card shows the type name → Close the drawer and reopen the map → Click Filter and check that the type's chip shows the name as text, without italics → Check that no injected script ran
  - Expected: A type name with HTML, an emoji and Arabic shows as plain text in the types drawer, the type picker, the location card and the filter chip.

- **zone names: whitespace-only saves as "<city> Zone", padded names are trimmed, HTML shows as text** — passed (map.spec.js:4199)
  - Where: Tools → Map, open the e2e city → Studio Zones drawer · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Open the Studio Zones drawer and click “Add Zone” → Type only spaces as Zone Name and click “Create Zone”; check that the zone is saved as “<city> Zone” → Add a zone named “   [e2e-map] Edge zone <time>   ” (spaces around); check that it is saved without the spaces → Add a zone named “[e2e-map] <u>zone</u> <time>”; check that it is saved as typed → Check that the HTML zone's row shows the name as text, without underline → Click “Map” on that row and reopen the map → Check that the toolbar's zone button shows the name as text, without underline (its text is recorded) → Delete the three zones
  - Expected: A spaces-only zone name falls back to “<city> Zone”, padded names are trimmed, and an HTML name is shown as plain text in the list and the toolbar.

- **two zones may share a name: both rows are listed** — passed (map.spec.js:4243)
  - Where: Tools → Map, open the e2e city → Studio Zones drawer · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Open the Studio Zones drawer and create “[e2e-map] Edge twin zone <time>” (Add Zone, 30 mi, Create Zone) → Create a second zone with exactly the same name → Note whether the server accepts the duplicate → Check that the drawer lists two rows with that name (one if the duplicate was refused) → Delete the zones with that name
  - Expected: Whether two zones may share a name is recorded, and the list matches the outcome: two rows when accepted.

- **a refused save keeps the form and its values; Update again succeeds** — passed (map.spec.js:4259)
  - Where: Tools → Map, open the e2e city → List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click List View, then Edit on the second base location's card → Type “[e2e-map] retry <time>” as the description → The test makes the server refuse the update with an error message → Click “Update Location” → Check that the error shows as a toast, the form stays open and the description is still filled in → The test lets updates through again → Click “Update Location” again → Check that the save succeeds with that description
  - Expected: A refused save keeps the form open with the user's input, and pressing Update again saves it.

- **a save answered HTTP 500 without an envelope is reported as an error** — bug · known bug isApiFailure is status === 0 only: a write answered HTTP 500 without an envelope ({ error, httpStatus: 500 } after the interceptor) toasts "Location updated successfully" and closes the form — confirmed run 3,isApiFailure is status === 0 only: a write answered HTTP 500 without an envelope ({ error, httpStatus: 500 } after the interceptor) toasts "Location updated successfully" and closes the form — confirmed run 3 (map.spec.js:4276)
  - Where: Tools → Map, open the e2e city → List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click List View, then Edit on the second base location’s card → Type “e2e 500” as the description → The test makes the update fail with a server error that has no message → Click “Update Location” and watch for toasts for the next few seconds → Check that no “Location updated successfully” toast appears → Check that the form stays open
  - Expected: A save that fails with a server error is reported as an error and the form stays open. Known bug: the app toasts “Location updated successfully” and closes the form.
  - Web error: Error: no success toast for a failed save

- **a save that fails on the network is reported and keeps the form** — passed (map.spec.js:4292)
  - Where: Tools → Map, open the e2e city → List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click List View, then Edit on the second base location's card → Type “e2e offline” as the description → The test makes the update fail on the network → Click “Update Location” → Check that an error toast appears within 30 seconds → Check that the form stays open
  - Expected: A save lost on the network is reported with a message and the form stays open.

- **refused deletes of a location, a zone and a type leave each in place** — passed (map.spec.js:4304)
  - Where: Tools → Map, open the e2e city → List View, Studio Zones and LOC Types · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e). The test creates a zone “[e2e-map] Edge keep zone <time>” and a type “[e2e-map] Edge keep type <time>”.
  - Steps: Create the zone in the Studio Zones drawer and the type in LOC Types → The test makes the server refuse every location, zone and type delete with an error message → Click List View and delete the first base location (trash, then Delete in “Delete Location”) → Check that the error shows as a toast and the card is still listed; close the drawer → Open the Studio Zones drawer, delete the keep zone, and check that its row is still listed; close it → Click “LOC Types”, delete the keep type (Delete in “Delete Location Type”), and check that the dialog closes and the type is still listed → The test lets deletes through again, then deletes the keep zone and the keep type
  - Expected: Refused deletes of a location, a zone and a type leave each of them in place.

- **deleting the last location of the selected List View type shows that type's empty state** — passed (map.spec.js:4343)
  - Where: Tools → Map, open the e2e city → LOC Types and List View · account: iPhone XRR
  - Setup: The group's e2e city and its two base locations (see MAP-S00e).
  - Steps: Click “LOC Types”, “Add Type”, type “[e2e-map] Edge lonely type <time>” and click Create; close the drawer → Click List View, Edit the second base location, pick the new type and click “Update Location”; check that it saves → Click the new type's tab in the List View → Delete the location's card (trash, then Delete in “Delete Location”) → Check that the drawer says “No <type> locations” → Check that it says “You haven't added any locations of type “<type>” to <city> yet.” → Reopen the map and delete the type in LOC Types
  - Expected: Deleting the last location of the selected type tab shows that type's own empty state, naming the type and the city.

### Map — edge: two users at once (slot b)

- **a location slot b adds appears in A's open List View without a reload** — passed (map.spec.js:4508)
  - Where: Tools → Map in two browsers: iPhone XRR on the e2e city with List View open; Lava Device in Classic view · account: iPhone XRR and Lava Device
  - Setup: Before the group (as iPhone XRR): removes leftover e2e data, adds the e2e city (named by Google, e.g. Alice Springs) from the Cities drawer's “Current Location” suggestion, and saves two locations “[e2e-map] Two-user one <time>” and “[e2e-map] Two-user two <time>” through the Classic full-page New Location form.
  - Steps: iPhone XRR: open Tools → Map on the e2e city and click List View → Lava Device: switch to Classic view and open the full-page New Location form with coordinates just south of the e2e city in the link → Lava Device: type “[e2e-map] B live <time>” as the name and click “Save Location” → iPhone XRR: without reloading, check that the new location's card appears in the open List View within 45 seconds
  - Expected: A location Lava Device adds shows up live in iPhone XRR's open List View.

- **a stale edit form: A saving after B's edit keeps B's change** — bug · known bug PUT /v2/map is a full replacement with no version check: A's stale form silently overwrites B's newer description — confirmed run 4 (A's save sent an empty description),PUT /v2/map is a full replacement with no version check: A's stale form silently overwrites B's newer description — confirmed run 4 (A's save sent an empty description) (map.spec.js:4517)
  - Where: Tools → Map in two browsers, both on the e2e city → List View · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city and its two locations (see MAP-CU05).
  - Steps: iPhone XRR: click List View, then Edit on the first location, and leave the form open → Lava Device: open the map on the e2e city, click List View, then Edit on the same location → Lava Device: type “[e2e-map] from B <time>” as the description, click “Update Location” and check that it saves; close the List View → iPhone XRR: in the still-open form, add “ A” to the name and click “Update Location”; check that it saves → Check that iPhone XRR's save keeps Lava Device's new description
  - Expected: Saving a form opened before another user's edit keeps that user's newer change. Known bug: each save replaces the whole record with no version check, so iPhone XRR's stale form silently overwrites Lava Device's description (it sent an empty one).
  - Web error: Error: A's save carries B's description

- **B deletes a location while A edits it: A's save is refused with a message** — bug · known bug dev accepts an edit of a location another user has just deleted (status 1, "pinned_location_updated") instead of refusing it — confirmed run 4,dev accepts an edit of a location another user has just deleted (status 1, "pinned_location_updated") instead of refusing it — confirmed run 4 (map.spec.js:4541)
  - Where: Tools → Map in two browsers, both on the e2e city → List View · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city and its two locations (see MAP-CU05).
  - Steps: iPhone XRR: click List View, then Edit on the second location, and leave the form open → Lava Device: open the map on the e2e city, click List View and delete that location (trash, then Delete in “Delete Location”); check that it succeeds → iPhone XRR: type “e2e edit of a deleted record” as the description and click “Update Location” → Check that a toast appears within 30 seconds → Check that the server refuses the save (its answer is recorded)
  - Expected: Saving an edit of a location another user has just deleted is refused, with a message. Known bug: the server accepts the edit and the app reports it as saved.
  - Web error: Error: an edit of a deleted location is refused

- **B deletes the zone drawn on A: A stops drawing it** — passed (map.spec.js:4561)
  - Where: Tools → Map in two browsers, both on the e2e city · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city and its two locations (see MAP-CU05).
  - Steps: iPhone XRR: in the Studio Zones drawer create “[e2e-map] Two-user zone <time>” (Add Zone, 30 mi, Create Zone) and close the drawer → iPhone XRR: reopen the map and check that the toolbar reads “Zone: <that zone>” → Lava Device: open the map on the e2e city, open the Studio Zones drawer and delete that zone; check that it succeeds → iPhone XRR: without reloading, check within 45 seconds that the toolbar no longer names the zone
  - Expected: When Lava Device deletes the zone iPhone XRR has drawn, iPhone XRR's map stops drawing it, live.

- **A and B create the same type name at the same moment** — bug · known bug type names have no server-side uniqueness check: two users creating the same name at once get two identical types — confirmed run 4 (both answered status 1),type names have no server-side uniqueness check: two users creating the same name at once get two identical types — confirmed run 4 (both answered status 1) (map.spec.js:4577)
  - Where: Tools → Map in two browsers, both on the e2e city → LOC Types · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city (see MAP-CU05).
  - Steps: Both: open the map on the e2e city and click “LOC Types” → Both: click “Add Type” and type the same name, “[e2e-map] Race type <time>” → Both: click “Create” at the same moment → iPhone XRR: reopen the map and click “LOC Types” → Check that exactly one type with that name is listed (both answers and the count are recorded) → iPhone XRR: delete every type with that name
  - Expected: Two users creating the same type name at the same moment end up with one type. Known bug: type names have no uniqueness check, so two identical types are created.
  - Web error: Error: expect(received).toBe(expected) // Object.is equality

- **B renames a type A's location uses: A sees the new type, the location keeps the old label** — passed (map.spec.js:4615)
  - Where: Tools → Map in two browsers, both on the e2e city → LOC Types and List View · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city and its two locations (see MAP-CU05).
  - Steps: iPhone XRR: in LOC Types create “[e2e-map] Rename me <time>” (Add Type, Create) and close the drawer → iPhone XRR: click List View, Edit the first location, pick the new type and click “Update Location”; check that it saves; close the drawer → iPhone XRR: open LOC Types and leave it open → Lava Device: open the map on the e2e city, click “LOC Types”, then the type's Edit button → Lava Device: rename it to “[e2e-map] Rename me <time> v2”, click Update, then Update in “Update Location Type”; check that it saves → iPhone XRR: without reloading, check within 45 seconds that the renamed type shows in the open drawer; close it → iPhone XRR: search for the first location, open its card and note which type it shows
  - Expected: iPhone XRR's drawer shows the renamed type live; what the location's card shows afterwards (types are stored by name, so the old label) is only recorded.

- **A and B add the same city at the same moment** — passed · known bug the duplicate-name check is client-side against each user's own list: two users adding the same city at once get two cities,the duplicate-name check is client-side against each user's own list: two users adding the same city at once get two cities (map.spec.js:4643)
  - Where: Tools → Map in two browsers, both with the browser location set near Tennant Creek → Cities drawer · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city (see MAP-CU05). Both browsers' location is moved near Tennant Creek for this test.
  - Steps: iPhone XRR: open Tools → Map; Lava Device: reopen the map → Both: open the Cities drawer and click Add on the “Current Location” suggestion → Both: click “Add City” in the Add City dialog at the same moment → iPhone XRR: reopen the map → Check that only one city exists at that spot (both answers and the count are recorded) → iPhone XRR: delete every copy of that city in the Cities drawer → Put the browser locations back (near Alice Springs for iPhone XRR, near Coober Pedy for Lava Device)
  - Expected: Two users adding the same city at the same moment end up with one city. Known bug: the duplicate check runs only in each user's own browser, so two cities are created.

- **B deletes the city A has open: A moves off it** — bug · known bug a city deleted by another user stays selected: map:city:deleted removes it from the list but AppProvider keeps it as selectedCity — confirmed run 4,a city deleted by another user stays selected: map:city:deleted removes it from the list but AppProvider keeps it as selectedCity — confirmed run 4 (map.spec.js:4696)
  - Where: Tools → Map in two browsers; Lava Device's browser location set near Tennant Creek · account: iPhone XRR and Lava Device
  - Setup: Lava Device adds a throwaway city near Tennant Creek from the “Current Location” suggestion at the start of the test.
  - Steps: Lava Device: open Tools → Map and add the city from the “Current Location” suggestion (Add, then Add City) → iPhone XRR: open Tools → Map on that new city → Lava Device: delete the city in the Cities drawer (Delete, then Delete in “Delete City”) → iPhone XRR: open the Cities drawer and check within 45 seconds that the city is no longer listed; close the drawer → iPhone XRR: check that the toolbar no longer shows the deleted city
  - Expected: When Lava Device deletes the city iPhone XRR has open, iPhone XRR moves off it. Known bug: the deleted city leaves the list but stays selected in the toolbar.
  - Web error: Error: expect(locator).toHaveCount(expected) failed

- **B pins in a city A is not viewing: A gets an unread badge that clears on opening it** — passed (map.spec.js:4723)
  - Where: Tools → Map in two browsers: iPhone XRR on the e2e city, Lava Device on its Coober Pedy e2e city · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city (see MAP-CU05). Lava Device uses its Coober Pedy e2e city, or adds it from the “Current Location” suggestion if it is missing.
  - Steps: Lava Device: open Tools → Map and make sure the Coober Pedy e2e city exists → iPhone XRR: open Tools → Map on the e2e city (Alice Springs) and note the number on the red unread badge of the floating Cities button once it stops changing → Lava Device: in Classic view, save “[e2e-map] B unread <time>” in the Coober Pedy city through the full-page New Location form → iPhone XRR: wait up to 45 seconds for the Cities unread number to go up → iPhone XRR: open the Cities drawer and check that the Coober Pedy row shows a red badge → iPhone XRR: select that city → Check that the Cities unread number drops back within 30 seconds and the Coober Pedy row has no red badge
  - Expected: A pin added in a city iPhone XRR is not viewing raises the unread count on Cities and a badge on that city’s row; opening the city clears it.

### Map — edge: slot b without map rights (User Management) › server-side enforcement (canvas)

- **slot b's ungated edits (hover card, drawn zone) are refused by the server** — bug · known bug dev accepts PUT /v2/map from a member without the Map Post right (backend permission check missing; the client gate is the only one) — confirmed run 3: the location PUT answered status 1,dev accepts PUT /v2/map from a member without the Map Post right (backend permission check missing; the client gate is the only one) — confirmed run 3: the location PUT answered status 1 (map.spec.js:5001)
  - Where: iPhone XRR: Settings → Admin Settings → User Management; Lava Device: Tools → Map on the e2e city · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city with its 60-mile zone and location (see MAP-PB01).
  - Steps: iPhone XRR: turn off Lava Device’s Admin switch, and turn Map View on and Map Post off → Lava Device: open the map on the e2e city, search for “[e2e-map] Rights pin”, open its card and close it → Lava Device: hover the pin’s marker and click Edit on the hover card → If the edit form opens: type “e2e unauthorised edit” as the description, click “Update Location” and note whether the server accepted it; if the permission prompt shows instead, answer No → Lava Device: reopen the map and check that the toolbar reads “Zone: [e2e-map] Rights zone …” → Lava Device: click the map inside the zone circle, a little right of the centre; if the zone card opens, click “Edit Zone” → If the zone form opens: choose “30 mi”, click “Update Zone” and note whether the server accepted it; if the permission prompt shows instead, answer No → Check that the server accepted neither edit → iPhone XRR: put Lava Device’s admin flag and Map rights back as they were
  - Expected: The two edits the app does not gate (hover card Edit, drawn zone Edit Zone) reach the server, which refuses them for a member without the Post right. Known bug: dev accepts the location edit (and saves it) from a member without the Post right.
  - Web error: Error: the server refuses map PUTs from a member without the Post right

### Map — edge: slot b without map rights (User Management)

- **demoted from admin but with the map Post right, slot b can still write** — passed (map.spec.js:4877)
  - Where: iPhone XRR: Settings → Admin Settings → User Management; Lava Device: Tools → Map on the e2e city · account: iPhone XRR and Lava Device
  - Setup: Before the group (as iPhone XRR): removes leftover e2e data; adds the e2e city (e.g. Alice Springs) from the “Current Location” suggestion, a 60-mile zone “[e2e-map] Rights zone <time>”, a type “[e2e-map] Rights type <time>” and a location “[e2e-map] Rights pin <time>” (Classic full-page form); notes Lava Device's Admin switch and, if it is not an admin, its Map Post and View rights.
  - Steps: iPhone XRR: in User Management turn off Lava Device's Admin switch → iPhone XRR: open Lava Device's Posting Rights, choose the Tools section, turn on View and Post on the Map row, and close it → Lava Device: open Tools → Map on the e2e city → Lava Device: open the Cities drawer and check that “Add Another City” is shown; close it → Lava Device: click “Pin New Location”, check that pin mode turns on, then click it again to leave → iPhone XRR: put Lava Device's admin flag and Map rights back as they were
  - Expected: Without the admin flag but with the Map Post right, Lava Device can still write: Add Another City is shown and pin mode works.

- **without the map Post right every gated control asks slot b to request rights** — passed (map.spec.js:4893)
  - Where: iPhone XRR: Settings → Admin Settings → User Management; Lava Device: Tools → Map on the e2e city · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city with its zone, type and location (see MAP-PB01).
  - Steps: iPhone XRR: turn off Lava Device's Admin switch, and in its Posting Rights (Tools section) turn Map View on and Map Post off → Lava Device: open Tools → Map on the e2e city; its app now reads no Post right on the Map → Lava Device: open the Cities drawer and check there is no “Add Another City”; click Delete on the e2e city and answer No to the permission prompt; if a “Current Location” card shows, click its Add and answer No → Lava Device: click “Pin New Location”, answer No, and check that pin mode stays off → Lava Device: search for “[e2e-map] Rights pin”, open its card, click Edit and answer No → Lava Device: in List View check the pin's card offers only View; click “New Location” and answer No; click View, then Edit in the detail drawer, and answer No → Lava Device: in the Studio Zones drawer check the zone row offers only Map and Details; click “Add Zone” and answer No; click Details, then Edit, and answer No → Lava Device: in LOC Types click “Add Type”, then the type's Edit and its Delete, answering No each time → Lava Device: click Search, type the pin's name and check that one result is listed → iPhone XRR: put Lava Device's admin flag and Map rights back as they were
  - Expected: Without the Map Post right every write control raises the permission prompt (always answered No) and the cards hide their edit buttons, while reading and search still work.

- **revoking Post while slot b has the map open: the next write is gated (after a reopen at the latest)** — passed (map.spec.js:4953)
  - Where: iPhone XRR: Settings → Admin Settings → User Management; Lava Device: Tools → Map on the e2e city · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city (see MAP-PB01).
  - Steps: iPhone XRR: turn off Lava Device’s Admin switch and turn on its Map View and Post rights → Lava Device: open Tools → Map on the e2e city and keep it open → iPhone XRR: turn off Lava Device’s Map Post right → Lava Device: click “Pin New Location”; if the permission prompt appears at once, answer No → If pin mode turned on instead, click “Cancel Pin Mode”; if that now asks for rights (the revoke arrived meanwhile), answer No → Lava Device: reopen the map and click “Pin New Location” → Check that the permission prompt appears, and answer No → iPhone XRR: put Lava Device’s admin flag and Map rights back as they were
  - Expected: Once the Post right is revoked, the next write is gated after a reopen at the latest; whether and when it already applies on the open map is only recorded.

- **without the map View right slot b loses the tile but a deep link still opens the map** — bug · known bug the map never checks view_access: a member without it still opens the map by URL — confirmed run 4 (same as MAP-A08),the map never checks view_access: a member without it still opens the map by URL — confirmed run 4 (same as MAP-A08) (map.spec.js:5064)
  - Where: iPhone XRR: Settings → Admin Settings → User Management; Lava Device: Tools, then the Map by a direct link · account: iPhone XRR and Lava Device
  - Setup: The group's e2e city (see MAP-PB01).
  - Steps: iPhone XRR: turn off Lava Device's Admin switch and turn off its Map View right → Lava Device: open Tools and check that there is no Map tile → Lava Device: open the Map by a direct link → Check that the map does not open within 90 seconds → iPhone XRR: put Lava Device's admin flag and Map rights back as they were
  - Expected: Without the Map View right Lava Device has no Map tile and a direct link does not open the map. Known bug: the map never checks the View right, so the direct link still opens it (as in MAP-A08).
  - Web error: Error: a member without view_access should not reach the map

- **slot b is back to its original rights** — passed (map.spec.js:5086)
  - Where: Settings → Admin Settings → User Management · account: iPhone XRR
  - Setup: The rights Lava Device had before the group, noted in the group's setup (see MAP-PB01).
  - Steps: Open Settings → Admin Settings → User Management → Find Lava Device's row → Check that its Admin switch is as it was before the group → If Lava Device was not an admin: open its Posting Rights, choose the Tools section, check that the Map Post switch is as before, and close it
  - Expected: Lava Device's admin flag (and, for a non-admin, its Map Post right) is back to what it was before the group.

### Map — stubbed states (the stub answers; nothing reaches the backend)

- **Share (email) opens Compose with the pin text, then Discard** — passed (map.spec.js:1014)
  - Where: Tools → Map, on the city that opens by itself · account: iPhone XRR
  - Setup: The test adds a fake location “[e2e-map] stub pin” (address “[e2e-map] stub address”) to the open city’s location list; nothing is saved.
  - Steps: Open Tools → Map → Search for “[e2e-map] stub pin” and open its card → Click Share, then “Share” (email) → Check that the Compose Email dialog opens (or the message “Email-ID is not available” when the account has no project mailbox) → Check that the email text contains “[e2e-map] stub pin” → Click Discard; if a confirmation asks first, confirm Discard → Check that Compose closes
  - Expected: Share by email opens Compose with the location’s text in it, and Discard closes it without sending and without keeping a draft.

### Map — two users (realtime, slot b)

- **a reorder by slot b makes A re-read its city list and show the new order** — passed (map.spec.js:3021)
  - Where: Tools → Map in two browsers, Cities drawer: iPhone XRR watches; Lava Device’s browser location is set near Tennant Creek for one step · account: iPhone XRR and Lava Device
  - Setup: Uses the Coober Pedy city Lava Device added in MAP-M01; Lava Device adds a second e2e city (Tennant Creek) from the Cities drawer’s “Current Location” suggestion.
  - Steps: Lava Device: open the Cities drawer and check that its two e2e cities sit next to each other (the test skips otherwise) → iPhone XRR: open Tools → Map on the Coober Pedy city → Lava Device: focus the drag handle of the upper e2e city, press Space, Arrow Down, Space → Check that Lava Device’s new order is saved → iPhone XRR: without reloading, check that the map reads the city list again → iPhone XRR: open the Cities drawer and check that the lower e2e city is now above the other → Lava Device: move it back the same way and check that the saved order equals the original one
  - Expected: A reorder by another user makes the open map read the city list again and show the new order; moving back restores the original order.

### Map — edge: saved inputs on e2e data

- **deleting a location type keeps the locations that used it, untyped** — bug · known bug deleting a location type also deletes the locations that used it (backend: the app sends only the type DELETE), although the dialog says they stay without a type label — confirmed run 4 (ES07 / ES15 lost a base location, ES17 twice),deleting a location type also deletes the locations that used it (backend: the app sends only the type DELETE), although the dialog says they stay without a type label — confirmed run 4 (ES07 / ES15 lost a base location, ES17 twice) (map.spec.js:4370)
  - Where: Tools → Map, open the e2e city · account: iPhone XRR
  - Setup: The group’s e2e city; the test adds its own location “[e2e-map] Edge typed loc <time>” through the full-page New Location form (Classic view).
  - Steps: Click “LOC Types”, “Add Type”, type “[e2e-map] Edge doomed type <time>” and click Create; close the drawer → Click List View, Edit the test’s location, pick the new type and click “Update Location”; check that it saves → Reopen the map, open LOC Types and click Delete on the new type → Check that the “Delete Location Type” dialog says “Those locations will appear without a type label”; click Cancel → Delete the type again and confirm with Delete; check that it is deleted → Reopen the map and check that the city still lists the test’s location (now without a type)
  - Expected: Deleting a location type keeps the locations that used it, untyped, as the delete dialog promises. Known bug: the location is deleted together with its type.
  - Web error: Error: the location survives its type

## Sheet bugs (3)

- **MAP-01** Pin info card shows the old name and address after Edit or Move — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open a location pin's info card. 2. Edit its name or move it and save. 3. Look at the card and the marker.
  - Expected: The card and marker show the saved values.
  - Actual: The card kept the old name and address until reload; the list view had the new values.

- **MAP-02** Holding a pin cluster does not show "Zoom in to move individual pins." — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Zoom out until several pins merge into one numbered cluster. 2. Press and hold the cluster bubble, as if to drag it.
  - Expected: The alert "Zoom in to move individual pins." appears (ZL-21015).
  - Actual: 15 Nov re-run: holding a "2" cluster and dragging moved one of the hidden pins and asked "Outside Paris … Move it here anyway?" (Cancel tapped, nothing moved). Still no "Zoom in…" alert.

- **MAP-03** Pin type chip is blank when a pin is saved without a type — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Pin Location and save a pin without choosing a Location Type. 2. Tap the pin to open its info card (or open List View).
  - Expected: The type chip reads "Unknown".
  - Actual: The chip shows only the pin icon with no text. Every pin created in the run without a type looked the same.
