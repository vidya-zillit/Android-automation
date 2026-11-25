# Web E2E — Costume & Sync

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (0)

## Sheet bugs (35)

- **CSS-01** A failed /meta load leaves the tool on its loader forever — High · Web: Not checked, Android: Not checked
  - Steps: 1. Open Costumes & Set Sync while GET /meta fails (one failure is enough).
  - Expected: Module opens; meta retried.
  - Actual: Spinner 'Loading Costumes & Set Sync' forever; /meta never asked again; only fix is reopening the tool.

- **CSS-02** Rights that load after 10 s never open the tool — High · Web: Not checked, Android: Not checked
  - Steps: 1. Open the tool when the project rights list takes >10 s (slow network, cold start). 2. Wait.
  - Expected: Tool opens when rights arrive.
  - Actual: Rights poll (20 × 500 ms) gives up; screen stays on loader with zero calls until reopened.

- **CSS-03** Rights changed while the tool is open are ignored — High · Web: Not checked, Android: Not checked
  - Steps: 1. Open the tool with posting rights. 2. Admin revokes posting (or view, or disables the tool). 3. Keep using the tool.
  - Expected: Controls update (web refetches on posting/viewing/download rights events and leaves the tool when view is revoked or tool disabled).
  - Actual: Viewer is only re-read on Load (tool open / project change): write buttons stay; a revoked viewer keeps browsing.

- **CSS-04** After a failed save the dialog is stuck busy (Save spins, Cancel disabled) — High · Web: Not checked, Android: Not checked
  - Steps: 1. Scene page > Edit. 2. Change a field, Save while the service refuses.
  - Expected: Error toast; Save/Cancel usable to retry.
  - Actual: saving never resets: Save keeps loading, Cancel greyed; only Esc closes and typed edits are lost. Same pattern in 'New change' (SceneReadiness.kt:251) and '+ Character' (SceneReadiness.kt:303).

- **CSS-05** Script review is thrown away when the scene count changes — High · Web: Not checked, Android: Not checked
  - Steps: 1. Upload script → Replace → review open. 2. Another device adds/deletes a scene (or tick 'extract cues' and Import).
  - Expected: Review (or cue progress) stays.
  - Actual: ScriptUpload is remember(open, ctx, existingScenes): the reload rebuilds it; dialog returns to 'Drop the script here', losing edits; with cues the progress view disappears.

- **CSS-06** Searching the inventory from page 2 keeps page 2: 'No costumes match' and no pager to get back — High · Web: Not affected, Android: Not checked
  - Steps: 1. Production with more than 50 pieces 2. Costumes > Next (Page 2 of 2) 3. Type a name that is on page 1 (e.g. 'velvet') in the search box
  - Expected: Results from page 1 shown (search starts at page 1)
  - Actual: Request goes with page=2: header '1 pieces in inventory', list 'No costumes match', and the pager is hidden (pages=1), so the user cannot go back except by clearing the search

- **CSS-07** Notification deep links (?open=, ?doc=&kind=) are ignored — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Open the tool via …/costume-set-sync?open=continuity (or a script-upload notification with doc/kind).
  - Expected: Lands on the named page; upload viewer opens on the named version.
  - Actual: Path '' → Dashboard; doc/kind never read.

- **CSS-08** Failed background refresh of an open scene shows 'That scene could not be found' — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open a scene. 2. Network blips while a frame arrives or the 30 s poll fires.
  - Expected: Scene stays on screen (Resource keeps data on silent failure).
  - Actual: Page replaced by 'That scene could not be found' until a later refresh succeeds; any open edit dialog vanishes.

- **CSS-09** Edit scene form is wiped by any refresh of the scene — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Scene page > Edit, type a new Location. 2. Another device changes the scene (or a take/cleaning lands, or the 30 s poll returns a changed record).
  - Expected: Typed values kept.
  - Actual: Form re-seeded from the server record (remember(open, scene)).

- **CSS-10** Alteration deadline typed with a one-digit hour (9:30) is silently dropped — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Costume page > Alteration 2. Issue + Required work 3. Deadline date 10/11/2026, time '9:30' 4. Request
  - Expected: Ticket filed with deadline 09:30 (the fitting forms accept '9:30')
  - Actual: deadlineIso() fails to parse '9:30' and returns null, so the alteration is filed with NO deadline and no warning

- **CSS-11** Edit character silently erases the cast number (and age) on non-numeric input and accepts negative/decimal cast numbers — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Characters > open Anna (cast 1) > Edit 2. Cast number 'abc' (or '-3', '1.5'); or Age 'abc' 3. Save
  - Expected: Refused like the list's cast box and the New character form ('A cast number is a whole number'); stored number unchanged
  - Actual: 'abc' -> PATCH cast_number null: Anna loses her cast number with a success toast. '-3' is stored as -3, '1.5' as 1.5. Age 'abc' likewise clears the age

- **CSS-12** Actor form wipes what was typed when the country-code list finishes loading — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Slow connection (country-code list takes ~1.5 s), first actor form of the session 2. Actors > Add 3. Start typing First name
  - Expected: Typed text stays
  - Actual: When the dial-code list arrives the form state is re-created (remember keyed on dialCodes) and the First name box goes blank; same for Edit (reverts to the record)

- **CSS-13** Edit change: after a failed Save the button stays busy until the dialog is reopened — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open a change > Edit 2. Save while the service refuses (offline / validation error)
  - Expected: Error toast and Save usable again (web resets busy)
  - Actual: Toast shows, but `saving` is only reset in the success callback, so Save keeps its spinner/disabled state; the user must close and reopen the dialog (losing edits) to retry

- **CSS-14** Budget line amount typed with a thousands separator is saved as £0 — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Budget > + Budget. 2. Description 'Gown hire', Amount (GBP) '1,200'. 3. Click Add.
  - Expected: The amount is read as 1,200, or Add stays disabled with a field error.
  - Actual: Add is enabled and POST /expenses is sent with amount 0 — a £0 line appears and nothing warns. Same for any text the box cannot parse ('12,50', '1 200', '£300').

- **CSS-15** Budget 'Download template' saves the sheet without download rights — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Sign in with posting but no download rights. 2. Budget > Upload budget sheet > Download template.
  - Expected: The request-download-access flow opens, as for every other export in the module.
  - Actual: The template is fetched (host.downloadBytes) and saved with no rights check.

- **CSS-16** Rights note text is never shown to a restricted viewer — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Open the tool without posting (or download) rights.
  - Expected: Info note explaining what is off, with request buttons.
  - Actual: Only 'Request posting/download access' buttons in the top bar; strings desktop_csync_rights_* are unused.

- **CSS-17** Legacy/unknown addresses land on the dashboard — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Open …/tickets, …/continuity/shot or …/anything.
  - Expected: tickets → Alterations; continuity/shot → Book (shot tab); unknown → Scene Breakdown.
  - Actual: All three show Dashboard / On Set.

- **CSS-18** Discard on the leave question doesn't continue to the clicked page — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Add a scene row and type. 2. Click Dashboard. 3. Discard.
  - Expected: Navigates to Dashboard.
  - Actual: Edits dropped, stays on the breakdown; second click needed.

- **CSS-19** Escape doesn't close the header search menu — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Type in 'Search scenes, characters, costumes'. 2. Press Esc.
  - Expected: Menu closes.
  - Actual: Menu stays open (only Enter handled; popup isn't focusable).

- **CSS-20** First-run chooser offers .fountain, then the review refuses it — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Empty production, pick 'pilot.fountain'. 2. Create.
  - Expected: Either read it or don't offer it.
  - Actual: Accepted on the form; review opens with 'That file type cannot be read…'. Web has the same mismatch.

- **CSS-21** Own writes echoed by the socket are re-read (no own-device skip) — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Save anything (e.g. a scene) with the list open.
  - Expected: One re-read (web skips its own device's frames).
  - Actual: Write's own reload plus the echo's reload: two reads per save.

- **CSS-22** Cost and measurement checks accept 'NaN' and 'Infinity' — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Costumes > + Costume (finance user) > Purchase cost 'Infinity', or Actors > Add > Height 'NaN' 2. Look at the field
  - Expected: Error under the field ('Enter a number'); Save blocked
  - Actual: No error; Save enabled; the value goes into the request body (cost as a non-JSON number, measurement as the text 'NaN')

- **CSS-23** An actor whose Age is typed as 'NaN' cannot be saved: 'Something went wrong.' and nothing is sent — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Character Breakdown > Actors > Add 2. First name 'Ann', Age 'NaN' (or 'Infinity') 3. Create
  - Expected: Actor created with no age (web sends age: null)
  - Actual: Toast 'Something went wrong.'; no request reaches the service; dialog stays open. toActorBody puts JsonPrimitive(NaN) which cannot be encoded as JSON

- **CSS-24** New character: negative age is saved, a decimal age is dropped without a word — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Characters > + Character 2. Name 'Lighthouse keeper', Age '-5' (or '34.5') 3. Create
  - Expected: Age never below 0 (web InputNumber min=0) and 34.5 kept as typed
  - Actual: -5 is stored as the age; '34.5' is sent as no age (toLongOrNull) with no message

- **CSS-25** Costume quantity accepts negative numbers (stored as -3) — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Costumes > + Costume 2. Name 'Velvet cape', Quantity '-3' 3. Add to inventory
  - Expected: Refused, or stored as at least 1
  - Actual: Piece created with quantity -3 (0 becomes 1, decimals truncated)

- **CSS-26** Rental 'Picked up' / 'Returned' can be sent twice by a double click — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Vendors & Rentals > Rentals. 2. On a slow network, double-click Returned on a rental.
  - Expected: One PATCH; the row's buttons are disabled while the write is in flight.
  - Actual: Two PATCH /rentals/<id> {status: RETURNED} calls (each toasts and triggers the server's event/notification twice).

- **CSS-27** Sharing the whole budget does not send the top sheet the web sends — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Budget > Full budget. 2. Share / send the budget from its record actions.
  - Expected: The top sheet as text: department rows by section with Above-the-line / Production / Post / Other subtotals, residuals, then GRAND TOTAL.
  - Actual: '<name> · Budget', 'Grand total: …', then one '<department title>: <total>' line per group — no section subtotals; BudgetModel.topSheetText exists but nothing calls it.

- **CSS-28** Printed budget cover is a day short for a shoot spanning the spring clock change — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Production in a DST zone (e.g. Europe/London) with start 1 Mar 2026, end 30 Apr 2026. 2. Budget > Full budget > Print / PDF.
  - Expected: 'Shoot from 01 Mar 2026 · 61 days'.
  - Actual: '· 60 days' — (end − start) / 86 400 000 truncates the 23 h left after the lost hour.

- **CSS-29** 'Completed today' misses tickets finished in the last hour of the 25-hour clock-change day — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Europe/London, 25 Oct 2026 (clocks go back). 2. Mark a cleaning ticket ready at 23:30. 3. Look at the Sink header and READY column.
  - Expected: It counts in 'N completed today' and shows in the READY column.
  - Actual: Not counted or shown: isSameDay uses local midnight + 24 h, which ends at 23:00 that day (on the spring day it also takes the first hour of the next day).

- **CSS-30** CAD and AUD amounts show the code ('CAD 1,200') where the web shows 'CA$1,200' / 'A$1,200' — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Add a budget line in CAD (offered in the line's Currency picker) for 1200. 2. Read the sheet and totals.
  - Expected: CA$1,200 (and A$1,200 for AUD), as the web.
  - Actual: CAD 1,200 / AUD 1,200 — fmtMoney only knows ₹ £ $ € ¥.

- **CSS-31** Cleaning card 'x ago' rounds halves down to even, unlike the web — Low · Web: Not affected, Android: Not checked
  - Steps: 1. A cleaning ticket with no ETA created 2 h 30 min ago. 2. Read its card on the Sink board.
  - Expected: '3h ago' (web); a ticket 30 s old reads '1m ago'.
  - Actual: '2 hours ago'; 30 s reads 'just now' — kotlin.math.round is round-half-even.

- **CSS-32** Rental Rate/day accepts negative and unparseable values (sent as -5 or silently 0) — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Vendors & Rentals > + Rental. 2. Fill costume, vendor, dates; Rate/day '-5' (or '12,50'). 3. Save.
  - Expected: A negative rate is refused; '12,50' is not silently saved as 0.
  - Actual: rate_per_day -5 is sent; '12,50' becomes 0 (toDoubleOrNull ?: 0.0). The field is free text.

- **CSS-33** Final Draft emoji entities (&#128512;) are shown as raw text in the script viewer — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Keep an .fdx whose text contains a numeric entity above U+FFFF (e.g. &#128512;). 2. View uploaded script.
  - Expected: 😀
  - Actual: '&#128512;' — the decoder only accepts code points up to U+FFFF.

- **CSS-34** A PDF with bytes before '%PDF' is shown as 'can't be shown here' — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Keep a call sheet PDF whose file starts with a newline before %PDF (some generators do this). 2. View uploaded call sheet.
  - Expected: The pages render (PDFBox and browser viewers accept a header within the first 1 KB).
  - Actual: Unsupported → 'This file can't be shown here' (only bytes 0–3 are compared with %PDF).

- **CSS-35** Budget upload: Import is greyed out without saying which row or why — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Upload a sheet. 2. Clear the description of a ticked line (or tick a line with no amount).
  - Expected: The reason is shown (web: 'Row N: needs a description and amount' as the button's tooltip).
  - Actual: Import is disabled with no explanation anywhere.
