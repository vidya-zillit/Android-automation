# Web E2E — Account Hub

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: written — 1,893 written cases in 11 sections; P0 automation in progress

## Web test cases (1931)

### Submit Day and purchase orders

- **Readiness banner @p0** — passed (account-hub/11-ad-dashboard-submit.spec.js:341)
  - Where: /film-tools/ad-dashboard
  - Steps: Open Submit Day with nobody present. → Mark one present. → Add a signature.
  - Expected: "Not ready. No artistes marked present yet — complete attendance before submitting." → "Sign-off needed. 1 of N confirmed present — sign in the rail to submit." → "Ready to submit. 1 of N confirmed present · no exceptions." Header "Submit Day & Post PO" disabled until ready.

- **AD signature: add, change, clear @p0** — passed (account-hub/11-ad-dashboard-submit.spec.js:373)
  - Where: /film-tools/ad-dashboard
  - Steps: "Add signature" → pick/draw in the e-signature picker. → "Change signature". → "Clear".
  - Expected: Preview image appears; while empty the hint "Add your signature to enable Submit Day & Post PO in the header." shows; "Clear" empties it and disables Submit again.

- **Day summary and PO cards per agency and currency (stubbed day) @p0** — failed (account-hub/11-ad-dashboard-submit.spec.js:404)
  - Where: /film-tools/ad-dashboard
  - Steps: Present: 2 Direct PAYE (GBP), 2 from linked Agency A (GBP), 1 from Agency A (EUR), 1 from name-only Agency B (GBP), 1 absent. → Open Submit Day.
  - Expected: "Day Summary" per currency with Direct PAYE and Agency ("{n} across {m} agencies") totals; "Absent · Blocked · Draft" exceptions. Cards "Purchase Order — Agency A" for GBP and EUR separately, refs PO-D<n>-<yyyymmdd>-<i>, chip "Draft"; Agency B chip "No vendor — PO skipped"; banner adds "Agency B has no linked vendor — their PO will be skipped." Route select "Purchase Orders — standard queue".
  - Web error: Error: expect(locator).toContainText(expected) failed

- **Submit happy path (agency artistes only, no e-mail) @p0** — failed (account-hub/11-ad-dashboard-submit.spec.js:488)
  - Where: /film-tools/ad-dashboard
  - Steps: Sign. → Click "Submit Day & Post PO". → Reopen Submit Day.
  - Expected: One POST /api/v2/purchase-orders per vendor-linked agency + currency: {vendor_id, department_id, status:'PENDING', currency, line_items:[{quantity:1, unit_price:<payTotal>, total}], description:'Supporting artistes — Day n · Agency A · PO-…-1'}; then POST /api/v2/ad-shoot-days/submit {signature:{signature_image, signed_at}, sign_status:'e-sign', po_reference, po_post_type:'standard', purchase_orders:[{po_id, agency, agency_id, currency, amount}], shoot_date}. Toast "Day n submitted · 2 POs posted · PO-… · 1 PO pending (no vendor)"; banner "Day n submitted · <time>." with "{n} artistes posted to the PO queue."; cards "Posted"; signature proof "Drawn signature"; route select disabled; no header Submit button. POs appear in Purchase Orders as PENDING.
  - Web error: Error: expect(received).toBe(expected) // Object.is equality

- **Submit happy path with external Direct PAYE artistes (approval e-mails) @p0** — skipped (account-hub/11-ad-dashboard-submit.spec.js:488)
  - Where: /film-tools/ad-dashboard
  - Steps: Sign. → Click "Submit Day & Post PO". → Reopen Submit Day.
  - Expected: One POST /api/v2/purchase-orders per vendor-linked agency + currency: {vendor_id, department_id, status:'PENDING', currency, line_items:[{quantity:1, unit_price:<payTotal>, total}], description:'Supporting artistes — Day n · Agency A · PO-…-1'}; then POST /api/v2/ad-shoot-days/submit {signature:{signature_image, signed_at}, sign_status:'e-sign', po_reference, po_post_type:'standard', purchase_orders:[{po_id, agency, agency_id, currency, amount}], shoot_date}. Toast "Day n submitted · 2 POs posted · PO-… · 1 PO pending (no vendor)"; banner "Day n submitted · <time>." with "{n} artistes posted to the PO queue."; cards "Posted"; signature proof "Drawn signature"; route select disabled; no header Submit button. POs appear in Purchase Orders as PENDING.

- **A failed PO blocks submit; the retry does not duplicate @p0** — passed (account-hub/11-ad-dashboard-submit.spec.js:547)
  - Where: /film-tools/ad-dashboard
  - Steps: Route the 2nd POST /purchase-orders → 500 {message}. → Submit. → Remove the route; Submit again.
  - Expected: Step 2 toast "PO for <agency> (<cur>) failed — <msg>. Day not submitted; fix and retry (created POs won't duplicate)."; no /ad-shoot-days/submit call. Step 3 creates only the failed PO, then submits.

### Manual entry form

- **Save draft vs Save & add to list @p0** — passed (account-hub/11-ad-dashboard-register.spec.js:177)
  - Where: /film-tools/ad-dashboard
  - Steps: Valid external artiste → "Save draft". → Another → "Save & add to list".
  - Expected: POST /api/v2/artistes with status:'draft' → toast "Artiste saved as draft"; with status:'pending' → "Artiste saved & added to today's list"; form resets; body has no shoot_date (see Open questions).

### Agency spreadsheet import

- **File type and preview @p0** — passed (account-hub/11-ad-dashboard-register.spec.js:239)
  - Where: /film-tools/ad-dashboard
  - Steps: Artistes Onboarding: drop a .pdf. → "Choose file" a 4-row .csv, click "Review extras".
  - Expected: .pdf → toast "Only .xlsx, .xls or .csv files." CSV shows "{size} KB · ready to review"; steps "Reading file", "Parsing rows", "Matching agencies & unions", "Validating details"; upload then POST /api/v2/artistes/import/preview {attachment}; header "4 extras extracted · {n} ready · {n} need fixing"; agency cell "Matched vendor · <id>" or "No matching vendor — saved as a plain name".

- **Commit to today's list; locked day; nothing ticked; empty file @p0** — passed (account-hub/11-ad-dashboard-register.spec.js:345)
  - Where: /film-tools/ad-dashboard
  - Steps: Click "Add 3 to today's list". → Submit today and preview again. → Untick all and click an add button. → Import an empty file.
  - Expected: POST /api/v2/artistes/import/commit-today {rows, shoot_date:<today UTC midnight>, timezone:'UTC'}; toast "Added 3 to today's list"; done "Saved as External · pending — on today’s list." 2: button replaced by "Today's day is submitted — extras can only be added to the register." 3: "Select at least one valid extra." 4: "No rows found in the file."

### Agencies

- **Add an agency: required fields, partial bank, save @p0** — passed (account-hub/11-ad-dashboard-register.spec.js:414)
  - Where: /film-tools/ad-dashboard
  - Steps: "Add Agency". → "Save & add to list" empty. → Fill required fields, enter only a bank name. → Add holder and IBAN; save.
  - Expected: Page "Rates & Config / Add agency", hint "Vendors created here will be marked Non-Verified." Errors "Vendor name is required", "Contact person is required", "Email is required", "Address line 1 is required", "City is required", "Postal code is required", "Country is required"; partial bank → "Account holder is required", "Enter an account number or an IBAN". Save → POST /api/v2/ad-rate-config/vendors; toast "Agency added"; lands on /config, not the Agency tab.

### Wrap reports and Exhibit G

- **Start a Wrap report from a template; autosave @p0** — failed (account-hub/11-ad-dashboard-register.spec.js:547)
  - Where: /film-tools/ad-dashboard
  - Steps: "+" (aria "New report") → "Choose a Report Template" → "Use <Wrap template>". → Edit a cell and wait 2 s.
  - Expected: URL /wrap-report/new?template=<id>; first autosave POST /wrap-reports with a client id and notify:false (no list refresh); /new is replaced by /<shootDate>; further edits PATCH with notify:false; the editor doesn't remount on the id swap.
  - Web error: Test timeout of 300000ms exceeded.

- **Publish and distribute; download right (2U) @p0** — failed (account-hub/11-ad-dashboard-register.spec.js:591)
  - Where: /film-tools/ad-dashboard
  - Steps: "Publish". → Confirm "Publish & distribute". → Open the published report as a user without download right.
  - Expected: Modal lists "Distribution list" with "{sent}/{total} recipients" (no-email users "no e-mail — won't receive") or "No distributors set in Rates & Config → Permissions & Distribution. …"; POST …/:id/post; toast "Report published · sent to {n}"; PDF shows at 100%; row "Published" with lock "Published reports can't be deleted"; day status "Published"; recipients get e-mail. Download button only with download right.
  - Web error: Error: expect(locator).toBeVisible() failed

- **Sign Exhibit G in person @p0** — skipped (account-hub/11-ad-dashboard-register.spec.js:667)
  - Where: /film-tools/ad-dashboard
  - Steps: Add a performer; on the never-saved sheet click "Sign here". → Draw and submit. → Click "Sign here" again; click it on an empty Cast row.
  - Expected: Sheet saves first ("Saving…"); PNG uploads to project storage; POST …/:id/sign {person_id, cell_id, row_id, signature_file, in_person:true}; row "Signed in person"; panel "Cast signatures", "{n} of {m} signed". Again → "That performer has already signed". Empty row → "No performer in this row — add one to the Cast cell before signing".

- **Publish is gated on signatures @p0** — skipped (account-hub/11-ad-dashboard-register.spec.js:693)
  - Where: /film-tools/ad-dashboard
  - Steps: With one unsigned performer click "Publish". → Route POST …/post → {message:'ad_report_signatures_pending', …outstanding}.
  - Expected: 1: toast "Signatures pending — 1 still to sign. Use "Sign here" on each cast row before publishing."; no modal. 2: modal "Waiting on 1 signature" (or "Report changed since signing") with "Open signatures".

### Extras portal (/extras-portal/:token)

- **Approve a submitted day (stubbed session and day) @p0 @smoke** — failed (account-hub/11-ad-portal.spec.js:222)
  - Where: /film-tools/ad-dashboard
  - Steps: Open the approval link ?approve_day=<dayId> and verify. → Click "Approve this Day". → Reopen the link.
  - Expected: GET /extras-portal/<token>/day/<dayId>; "Day voucher / <date>", "Your day", Call time / Wrap time in UTC, "Pay breakdown", "Day Total", "Awaiting your approval — payroll cannot process until approved."; POST …/day/<dayId>/approve ("Approving…"); "Day approved. The production team has been notified — nothing else to do." and "View your details on the portal →". Reopened: status "Approved by you", no button. AD's Today's List shows "✓ Signed".
  - Web error: Error: expect(locator).toBeVisible() failed

### Access & navigation › as a user with no AD view (crew, row stubbed off)

- **User with no view right is bounced @p0 @smoke** — passed (account-hub/11-ad-dashboard.spec.js:116)
  - Where: /film-tools/ad-dashboard · account: user
  - Steps: Sign in as a user whose ad_dashboard_tool row has no view_access. → Open /film-tools/ad-dashboard/todays-list.
  - Expected: Redirected to /film-tools (replace); no /api/v2/artistes or /api/v2/supporting-artist-days call.

### Access & navigation

- **Tile opens the tool on Wrap Report @p0 @smoke** — passed (account-hub/11-ad-dashboard.spec.js:94)
  - Where: /film-tools/ad-dashboard · account: posting AD
  - Steps: Sign in as posting AD. → Film Tools → click "AD Dashboard".
  - Expected: URL becomes /film-tools/ad-dashboard/wrap-report (replace). Sidebar shows header "AD Dashboard", segmented control "Reports" / "Artistes" with "Reports" selected, items "Wrap Report" (active) and "SAG-Aftra Exhibit G".

### Rights (read-only AD = crew + view, no posting) › viewer pages (crew + stubbed view-only)

- **Hidden pages redirect by URL @p0 @smoke** — passed (account-hub/11-ad-dashboard.spec.js:198)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: As viewer open /live-day, /onboarding, /onboarding/manual, /config, /config/vendor, /config/union, /templates.
  - Expected: Each is replaced by /film-tools/ad-dashboard/todays-list.

- **Schedule and report lists for a viewer (stubbed rows) @p0 @smoke** — passed (account-hub/11-ad-dashboard.spec.js:310)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: As viewer open Shoot Schedules. → Open Wrap Report and SAG-Aftra Exhibit G.
  - Expected: Schedule rows not clickable, no chevron; a submitted day shows no "Generate report"; a published day shows "View report". Report lists show published rows only, no Status filter group, no "+" or "Create …", no delete.

- **A draft report is not opened by deep link; a posted one is (stubbed reports) @p0 @smoke** — passed (account-hub/11-ad-dashboard.spec.js:364)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: As AD create a draft Wrap report for day D. → As viewer open /film-tools/ad-dashboard/wrap-report/<D>.
  - Expected: The editor does not show the draft (closes to the list). Repeat with a posted report: its PDF opens.

### Today's List

- **Est. Gross adds flat additionals to the engine gross (stubbed day) @p0 @smoke** — passed (account-hub/11-ad-dashboard.spec.js:555)
  - Where: /film-tools/ad-dashboard
  - Steps: Route GET /api/v2/supporting-artist-days to return today's Direct PAYE row with gross_amount:120, allowances:[{rate_type:'flat',rate_amount:10},{rate_type:'pct',rate_amount:5}], additional_fees:[{amount:25}], currency:'GBP'. → Open Today's List.
  - Expected: Est. Gross "£155.00" (120 + 10 + 25; the percentage line carries no cash); Est. Day Cost includes 155.

### Extras portal (/extras-portal/:token)

- **The code gate comes first; a signed-in colleague stays signed in @p0 @smoke** — passed (account-hub/11-ad-portal.spec.js:84)
  - Where: /film-tools/ad-dashboard
  - Steps: In a fresh context open /extras-portal/<token>. → Open it in a tab where a colleague is signed in.
  - Expected: "STEP 1 OF 2", "Verify Your Identity", header "Supporting Artiste Portal", badge "Artiste Portal", intro "Your details are protected. …"; no GET /extras-portal/<token> before verifying; "Send Verification Code" disabled until the e-mail is valid. The colleague stays signed in.

- **Verify by e-mailed code (stubbed request-otp / verify-otp) @p0 @smoke** — passed (account-hub/11-ad-portal.spec.js:116)
  - Where: /film-tools/ad-dashboard
  - Steps: Enter the artiste's e-mail (any case) → "Send Verification Code". → Paste the 6-digit code → "Verify & Continue".
  - Expected: POST /extras-portal/<token>/request-otp {email}; step 2 "We sent a 6-digit code to a****@domain. It expires in 15 minutes and can be used once." (masked address from the response); "Resend in 30s" then "Resend code". POST verify-otp {email, otp}; sessionStorage sa_portal_session:<token> holds the bearer; later calls send Authorization: Bearer and x-portal-otp; strip "Session expires in mm:ss".

- **Verify by e-mailed code (real e-mail) @p0** — skipped (account-hub/11-ad-portal.spec.js:165)
  - Where: /film-tools/ad-dashboard
  - Steps: Enter the artiste's e-mail (any case) → "Send Verification Code". → Paste the 6-digit code → "Verify & Continue".
  - Expected: POST /extras-portal/<token>/request-otp {email}; step 2 "We sent a 6-digit code to a****@domain. It expires in 15 minutes and can be used once." (masked address from the response); "Resend in 30s" then "Resend code". POST verify-otp {email, otp}; sessionStorage sa_portal_session:<token> holds the bearer; later calls send Authorization: Bearer and x-portal-otp; strip "Session expires in mm:ss".

- **Detail, update, thank-you (stubbed session and portal API) @p0 @smoke** — passed (account-hub/11-ad-portal.spec.js:170)
  - Where: /film-tools/ad-dashboard
  - Steps: After verifying, read "Your details". → "Update details", change the phone, attach a requested document. → "Submit details".
  - Expected: GET /extras-portal/<token>; detail view with "Setup progress"; form "Complete your details" with AD-named documents under "Required documents" (labels locked). Files go one by one to POST /extras-portal/<token>/upload, then PATCH /extras-portal/<token>. "Thank you!" + "Taking you to your details in {n}s…", back to the detail after 10 s (or "View my details"). AD's register shows the change.

- **Detail, update, thank-you (real e-mail; AD register shows the change) @p0** — skipped (account-hub/11-ad-portal.spec.js:216)
  - Where: /film-tools/ad-dashboard
  - Steps: After verifying, read "Your details". → "Update details", change the phone, attach a requested document. → "Submit details".
  - Expected: GET /extras-portal/<token>; detail view with "Setup progress"; form "Complete your details" with AD-named documents under "Required documents" (labels locked). Files go one by one to POST /extras-portal/<token>/upload, then PATCH /extras-portal/<token>. "Thank you!" + "Taking you to your details in {n}s…", back to the detail after 10 s (or "View my details"). AD's register shows the change.

- **Approve a submitted day (real approval e-mail; AD's list shows ✓ Signed, 2U) @p0** — skipped (account-hub/11-ad-portal.spec.js:256)
  - Where: /film-tools/ad-dashboard
  - Steps: Open the approval link ?approve_day=<dayId> and verify. → Click "Approve this Day". → Reopen the link.
  - Expected: GET /extras-portal/<token>/day/<dayId>; "Day voucher / <date>", "Your day", Call time / Wrap time in UTC, "Pay breakdown", "Day Total", "Awaiting your approval — payroll cannot process until approved."; POST …/day/<dayId>/approve ("Approving…"); "Day approved. The production team has been notified — nothing else to do." and "View your details on the portal →". Reopened: status "Approved by you", no button. AD's Today's List shows "✓ Signed".

### Rights (read-only AD = crew + view, no posting) › viewer, stubbed rights

- **Read-only user: hidden nav, no header actions (no posting key on the row) @p0 @smoke** — passed (account-hub/11-ad-dashboard.spec.js:143)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: Sign in as viewer (no posting row at all, then a row with posting_access: false). → Open the tool; click "Artistes". → Admin grants posting (fires posting_rights_updated).
  - Expected: "Run Current Day", "Artistes Onboarding", "Rates & Config" absent; "Artistes" lands on "Shoot Schedules". No "Add Direct", "Import List", "Upload Agency List", "Add Direct Extra", "Manual Entry", "Save changes", "Submit Day & Post PO" anywhere. After step 3, GET /v2/project/tools refetches and the items appear without reload.

- **Read-only user: hidden nav, no header actions (posting_access false) @p0 @smoke** — passed (account-hub/11-ad-dashboard.spec.js:143)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: Sign in as viewer (no posting row at all, then a row with posting_access: false). → Open the tool; click "Artistes". → Admin grants posting (fires posting_rights_updated).
  - Expected: "Run Current Day", "Artistes Onboarding", "Rates & Config" absent; "Artistes" lands on "Shoot Schedules". No "Add Direct", "Import List", "Upload Agency List", "Add Direct Extra", "Manual Entry", "Save changes", "Submit Day & Post PO" anywhere. After step 3, GET /v2/project/tools refetches and the items appear without reload.

### Rights (read-only AD = crew + view, no posting) › viewer pages (crew + stubbed view-only)

- **Shoot-day pages are read-only for a viewer @p0** — passed (account-hub/11-ad-dashboard.spec.js:209)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: As viewer open Today's List, Add Extras, Attendance, Additionals, Submit Day for a day with artistes. → Open a Direct PAYE voucher.
  - Expected: No "Add Extra", "Mark present", "Mark out", attendance buttons or bulk bar; Additionals tiles/inputs disabled; Add Extras shows only the list; Submit Day banner "Read only. This shoot day isn't open for submission here." and signature slot "Not yet signed". Voucher opens without "Send signing link".

- **Register and artiste page are read-only for a viewer @p0** — passed (account-hub/11-ad-dashboard.spec.js:278)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: As viewer open Artiste Register. → Open an artiste.
  - Expected: Rows show "View" but no "Delete". Artiste page shows no "Verify artiste", "Block", "Unblock", "Copy portal link" or "Edit details".

### Rights (read-only AD = crew + view, no posting)

- **Posting granted in the Permission Grid shows the items without reload (2U) @p0** — skipped (account-hub/11-ad-dashboard.spec.js:163)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: Sign in as viewer (no posting row at all, then a row with posting_access: false). → Open the tool; click "Artistes". → Admin grants posting (fires posting_rights_updated).
  - Expected: "Run Current Day", "Artistes Onboarding", "Rates & Config" absent; "Artistes" lands on "Shoot Schedules". No "Add Direct", "Import List", "Upload Agency List", "Add Direct Extra", "Manual Entry", "Save changes", "Submit Day & Post PO" anywhere. After step 3, GET /v2/project/tools refetches and the items appear without reload.

### Run Current Day and meals

- **Gated five-section runner @p0** — bug (account-hub/11-ad-dashboard.spec.js:812)
  - Where: /film-tools/ad-dashboard
  - Steps: Open Run Current Day with two unmarked artistes. → Mark one present and one absent.
  - Expected: "Day Progress" with cells Attendance, Mid-Day, Wrap, Additionals, Submit and "{done} of 5 phases complete"; Mid-Day/Wrap/Additionals read "Locked" and don't open, Submit locked; button "Mark everyone first" disabled, then "Go to Mid-Day". Header shows "{total} on call · {present} present" and a "Today's List" button.
  - Web error: Error: Submit locked with nobody marked

- **Day meals: add, save, log, short break @p0** — passed (account-hub/11-ad-dashboard.spec.js:855)
  - Where: /film-tools/ad-dashboard
  - Steps: Mid-Day → "Add meal" (type "Lunch"). → Break start 12:00, Break end 12:30, "Save meals". → Change end to 12:20 and save.
  - Expected: "Save meals" appears only when dirty; PATCH /api/v2/ad-shoot-days/today {meals:[{order:1,type:'Lunch',start_time:<12:00Z>,end_time:<12:30Z>}], shoot_date} with no list refetch; card "Logged · 12:00–12:30 · 0h 30m · on schedule"; 12:20 shows "short break". Artiste Meals rows mirror the meal at once.

- **Wrap section: mark out, specific time, mark all out @p0** — passed (account-hub/11-ad-dashboard.spec.js:895)
  - Where: /film-tools/ad-dashboard
  - Steps: Wrap → "Mark out" one row. → Chevron "Enter a specific out time" 19:30, "Save". → "Mark all out" ("{n} not wrapped").
  - Expected: Toasts "{name} wrapped at HH:mm" and "{n} wrapped at HH:mm"; "Wrap everyone first" stays disabled until every present artiste has a time out, then reads "Go to Additionals". With everyone absent it is enabled at once.

### Shoot Schedules and opened days

- **Open today, past, future and submitted days; submit a past day @p0** — failed (account-hub/11-ad-dashboard-submit.spec.js:62)
  - Where: /film-tools/ad-dashboard
  - Steps: Click today's row. → A past unsubmitted row. → A future row. → A submitted row. → On the past day go to Submit, sign, click the header "Submit Day & Post PO".
  - Expected: 1 → /live-day. 2 → /schedule/<epoch>, header "Schedule / Shoot Day", "Shoot Day n · <date>", banner "Editing Day n." and all five sections open (no locks). 3 → "Read only — this shoot day is in the future." plus a "Read only" tag. 4 → "This day is submitted — read only." 5 → POST /api/v2/ad-shoot-days/submit with that day's shoot_date; page turns read-only.
  - Web error: Error: submit day answered {"http":400,"ok":false,"status":0,"message":"unwrapped_artistes_present","messageElements":[],"data":{"count":1,"unwrapped":[{"day_id":"6afba5a2a753bc7eeb9313e6","artiste_id

- **Edits on a past day anchor on that day @p0** — failed (account-hub/11-ad-dashboard-submit.spec.js:129)
  - Where: /film-tools/ad-dashboard
  - Steps: On a past day mark present at 07:00 and save a meal.
  - Expected: call_time = Date.UTC(<that day>, 07:00), not today; meal PATCH carries that day's shoot_date.
  - Web error: Error: expect(received).toEqual(expected) // deep equality

- **Generate a report from a submitted day @p0** — failed (account-hub/11-ad-dashboard-submit.spec.js:167)
  - Where: /film-tools/ad-dashboard
  - Steps: On a submitted row click "Generate report". → Pick a Wrap template in "Generate from". → Repeat with an AD template on the last row of a long list.
  - Expected: Menu lists no Exhibit G templates (origins "Provided by Zillit", "Zillit draft", "Sample format", "Custom") and stays inside the viewport. Opens /wrap-report/<sd>?template=<id> (AD template → /ad-report/…); GET …/by-day then POST to that kind's base path.
  - Web error: Error: submit day answered {"http":400,"ok":false,"status":0,"message":"unwrapped_artistes_present","messageElements":[],"data":{"count":1,"unwrapped":[{"day_id":"6afba5c1a753bc7eeb9314c8","artiste_id

### Additionals

- **Toggle allowance and fee tiles @p0** — passed (account-hub/11-ad-dashboard-submit.spec.js:224)
  - Where: /film-tools/ad-dashboard
  - Steps: Open Additionals ("Allowances — toggle per artiste" banner). → Expand an artiste, click an allowance tile, then a fee tile.
  - Expected: PATCH {allowances:[…]} then {additional_fees:[…]}; toast "{name} — additionals updated"; header chips and "Est. Gross" update after the refetch about 900 ms later.

- **Custom payment rules @p0** — failed (account-hub/11-ad-dashboard-submit.spec.js:264)
  - Where: /film-tools/ad-dashboard
  - Steps: Description "Stunt adj", amount 0 → Save. → Amount 50 → Save. → Change to "50.00". → Amount 0 → Save. → Blank description, amount 30 → Save. → Route PATCH → 500 and Save.
  - Expected: 1: Save disabled. 2: additional_fees gets {identifier:'custom', label:'Stunt adj', rate_type:'flat', rate_amount:50, amount:50, basis:'event'}. 3: Save disabled (numerically equal). 4: line removed and description cleared. 5: label "Custom payment". 6: error toast, draft kept for retry, chips unchanged.
  - Web error: Error: expect(locator).toHaveValue(expected) failed

### Artiste Register and artiste page

- **Actions by status; bank block for accountant and non-accountant (2U) @p0** — passed (account-hub/11-ad-dashboard-register.spec.js:61)
  - Where: /film-tools/ad-dashboard
  - Steps: Open a pending external, a verified, a blocked, a draft and an internal artiste. → Open one with bank details as accountant and as non-accountant. → Open /register/badid.
  - Expected: Pending: "Verify artiste", "Copy portal link", "Edit details", "Block". Verified: no Verify. Blocked: only "Unblock". Draft: "Edit details", plus "Copy portal link" only when the server sends portal_link_eligible. Internal: never "Copy portal link". Accountant sees masked bank values (last four) with "Reveal" that re-hides after 5 s; non-accountant gets no bank fetch. Bad id: "Artiste not found."

- **Verify, block, unblock @p0** — passed (account-hub/11-ad-dashboard-register.spec.js:120)
  - Where: /film-tools/ad-dashboard
  - Steps: "Verify artiste". → "Block". → "Unblock".
  - Expected: POST /artistes/:id/verify → toast "Artiste verified"; /block body {reason:null} → "Artiste blocked"; /unblock → "Artiste unblocked"; GET /artistes/:id after each.

### Manual entry form

- **Identity and Category are required @p0 @smoke** — passed (account-hub/11-ad-dashboard-register.spec.js:153)
  - Where: /film-tools/ad-dashboard
  - Steps: Open /onboarding/manual (header "Manual Entry") or Register "Add Direct Extra". → Click "Save & add to list" empty. → Toggle "External artiste", fill first name only.
  - Expected: Inline "Select a crew member.", "First name is required.", "Last name is required.", Category "Required."; page scrolls to Personal (identity missing) or Booking (only category missing); no POST. Category carries an asterisk.

### Agency spreadsheet import

- **Row validation and inline fix (stubbed upload + preview) @p0** — passed (account-hub/11-ad-dashboard-register.spec.js:269)
  - Where: /film-tools/ad-dashboard
  - Steps: Preview rows missing a last name, missing an email, with "bad@x". → Fix one inline.
  - Expected: Rows marked "Needs review" with "Name (first + last) required", "Email required", "Invalid email", unticked. A fixed row turns "Ready" and auto-ticks unless its tick was touched. Add buttons count only ticked valid rows.

- **Commit to the register @p0** — passed (account-hub/11-ad-dashboard-register.spec.js:309)
  - Where: /film-tools/ad-dashboard
  - Steps: Click "Add 3 to Register". → "Upload another list".
  - Expected: "Adding to register…"; POST /api/v2/artistes/import/commit {rows}; toast "Added 3 extras to the register"; done "3 extras added to register · Saved as External · draft — review them in the register." ("{n} failed" when returned); step 2 resets.

### Rates & Config

- **Non-union rates and day types @p0** — passed (account-hub/11-ad-dashboard-register.spec.js:455)
  - Where: /film-tools/ad-dashboard
  - Steps: Day rate 150 for "Walk-on". → "Add day type" SWD, work 10h, meal 1h (−/+ 15-min steps). → Add a custom type "Night" and one empty row. → Add an "OT — after N hours worked" rule and an allowance with a nominal code. → Save.
  - Expected: day_rates lists all 8 categories (blank → 0, Walk-on 150); day_types:[{day_type:'SWD', work_min:600, meal_break_min:60}, {day_type:'Night', …}], empty row dropped; overtimes and allowances included; toast "Rate config saved".

### as crew

- **10 Check: the crew account sees its tools** — passed (account-hub/00-baseline.spec.js:663)

### 

- **01 Production Setup foundations: company, currencies, tags, tax types, banks** — passed (account-hub/00-baseline.spec.js:131)

- **02a Crew tool rights in the Viewing & Posting Rights Grid** — passed (account-hub/00-baseline.spec.js:223)

- **02b Module entry setups: Invoices, Cards, Petty Cash, Payroll team rights** — passed (account-hub/00-baseline.spec.js:273)

- **03 Approvers: default chain per module (Default → accountant, Amount ≥ N → crew)** — passed (account-hub/00-baseline.spec.js:395)

- **04 Budget import seeds the Chart of Accounts** — passed (account-hub/00-baseline.spec.js:463)

- **05 Forms Configuration: PO and float-request templates load (defaults kept)** — passed (account-hub/00-baseline.spec.js:502)

- **06 Vendors: one with fake bank details, one without** — passed (account-hub/00-baseline.spec.js:514)

- **07 Deal Memo Setup: one saved Union setup** — passed (account-hub/00-baseline.spec.js:547)

- **08 Time Card configuration: crew-controlled model** — passed (account-hub/00-baseline.spec.js:612)

- **09 Check: the Production Setup tour has no gaps left** — passed (account-hub/00-baseline.spec.js:629)

### Access & navigation

- **Accountant opens the console from the hub @p0 @smoke** — passed (account-hub/05-cards-access.spec.js:91)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: Log in as ACC100. → Account Hub sidebar → "Production Expense Cards".
  - Expected: URL /film-tools/account-hub/card-expenses; hub sidebar hidden; card sidebar titled "Production Expense Cards" with "Overview" and sections "CARDS", "WORKFLOW", "MANAGEMENT"; Overview page loads (GET /overview).

- **Accountant entering from the tool tile gets the crew view @p0 @smoke** — passed (account-hub/05-cards-access.spec.js:111)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100, Film Tools → "Production Expense Cards" tile.
  - Expected: URL ends /my-receipts; header "Production Expense Cards" with "View your receipts, card details, and approval status."; tabs "My Transactions", "Card", "Card Extension"; no accountant sidebar; card tiles show "Edit", never "Review".

- **Crew lands on My Transactions and switches tabs @p0 @smoke** — passed (account-hub/05-cards-access.spec.js:139)
  - Where: /film-tools/account-hub/card-expenses · account: CREW
  - Steps: As CREW, open /film-tools/account-hub/card-expenses. → Click "Card", then "Card Extension". → Press browser Back.
  - Expected: Redirected to /my-receipts; no "Approval Queue" / "Coding Queue" tabs unless the roles apply; the URL follows each tab; Back returns to /cards with "Card" highlighted.

- **Settings link shows for seniors only @p0 @smoke** — skipped (account-hub/05-cards-access.spec.js:162)
  - Where: /film-tools/account-hub/card-expenses · account: SEN
  - Steps: As SEN, open the console. → Repeat as PA (no Team row). → Repeat as ACC100.
  - Expected: "Settings" is listed under MANAGEMENT for SEN and PA; absent for ACC100.

- **Crew "Approval Queue" tab only for approvers @p0** — passed (account-hub/05-cards-access.spec.js:177)
  - Where: /film-tools/account-hub/card-expenses · account: APR1
  - Steps: As APR1, open the tool tile. → As CREW, open it.
  - Expected: APR1 sees "Approval Queue"; CREW does not.

- **Crew "Coding Queue" tab needs coordinator AND coding required @p0** — passed (account-hub/05-cards-access.spec.js:193)
  - Where: /film-tools/account-hub/card-expenses · account: COORD
  - Steps: As COORD, open the tile with coordinator coding on. → Senior turns coding off in Settings (both switches). → Reload as COORD.
  - Expected: Step 1: "Coding Queue" tab shown. Step 3: tab hidden.

- **View gate on tile and hub entry @p0 @smoke** — passed (account-hub/05-cards-access.spec.js:233)
  - Where: /film-tools/account-hub/card-expenses · account: NOTOOL
  - Steps: As NOTOOL, open /film-tools/account-hub/card-expenses?entry=tool. → As NOHUB, open /film-tools/account-hub/card-expenses.
  - Expected: Both redirected to /film-tools without card data rendering.

### Receipts (upload, match, missing receipts)

- **Attach a suggested match @p0** — passed (account-hub/05-cards-receipts.spec.js:309)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On a "Match Suggested" row, kebab → "Attach".
  - Expected: POST /receipts/:id/confirm-match; row badge changes after refetch.

- **Manual match @p0** — failed (account-hub/05-cards-receipts.spec.js:335)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Kebab → "Manual Match". → Pick a candidate, "Confirm Match".
  - Expected: GET /receipts/:id/match-candidates then POST /receipts/:id/manual-match {transactionId}; with no candidates: "No candidate transactions found within ±5 days." and Confirm disabled.
  - Web error: Error: candidate £41.57 offered

### Coding & coordinator review

- **Coordinator codes and submits @p0** — passed (account-hub/05-cards-receipts.spec.js:369)
  - Where: /film-tools/account-hub/card-expenses · account: COORD
  - Steps: CREW uploads a receipt with no code. → As COORD, "Coding Queue" → open it ("Code Receipt"). → Cost Code, "Submit for Approval".
  - Expected: Receipt appears as "Pending Code"; POST /receipts/:id/submit-for-approval with nominal_code, card_id, currency; it leaves the queue; CREW sees "Awaiting Approval".

- **Coordinator who is also an approver @p0** — passed (account-hub/05-cards-receipts.spec.js:397)
  - Where: /film-tools/account-hub/card-expenses · account: COORDAPR
  - Steps: As COORDAPR, open a receipt in Coding Queue.
  - Expected: Green "Approve & Submit" replaces "Submit for Approval"; it sends POST /receipts/:id/approve-and-submit; the server decides how many levels it approves.

### Submit & approval levels (receipts)

- **Accountant queue: next approver approves @p0** — passed (account-hub/05-cards-receipts.spec.js:426)
  - Where: /film-tools/account-hub/card-expenses · account: accountant
  - Steps: As an accountant who is Level 1 approver, "Approval Queue". → "Approve" on the row.
  - Expected: POST /approvals/:id/approve {tier_number: approvals+1, user_id}; row leaves after refetch.

- **Accountant queue receipt detail actions @p0** — skipped (account-hub/05-cards-receipts.spec.js:442)
  - Where: /film-tools/account-hub/card-expenses · account: OVR
  - Steps: Open a row as next approver. → As OVR (not next approver). → As ACC100 (neither).
  - Expected: Footer "Reject" + "Approve"; "Override"; "No actions available". "History" is present and "Query" is never shown in this queue.

- **Override a receipt @p0** — failed (account-hub/05-cards-receipts.spec.js:468)
  - Where: /film-tools/account-hub/card-expenses · account: OVR
  - Steps: As OVR with "Accountant Override — Receipts" on, click "Override" on a row. → Turn the switch off and reload.
  - Expected: POST /approvals/:id/override with reason "Accountant override"; with the switch off the row shows "—".
  - Web error: Error: approve receipt tier 1: HTTP 400 card_expense_receipt_not_reconciled — receipt awaiting_approval/suggested_match txn 5506a67b-997b-45b8-b7c3-864a9fd49a4f

- **Select-all includes rows hidden by search @p0** — failed (account-hub/05-cards-receipts.spec.js:492)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: With several rows, type a search that leaves one visible. → Tick the header checkbox. → Read the bar count.
  - Expected: Expected: 1 selected. Code selects every unlocked row in the queue (toggleSelectAll uses items, not the filtered list), so a bulk action hits receipts the user cannot see.
  - Web error: Error: expect(locator).toHaveText(expected) failed

### Posting & seniority / posting limits

- **Posting limit below the amount @p0** — skipped (account-hub/05-cards-posting.spec.js:161)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100 (limit 100), open an approved receipt of 150 assigned to them. → Open one of 80.
  - Expected: 150: no Post; "Escalate" and "Submit for Review" shown. 80: "Post to Ledger" shown.

- **Submit-only and unlimited limits @p0** — failed (account-hub/05-cards-posting.spec.js:169)
  - Where: /film-tools/account-hub/card-expenses · account: ACC0
  - Steps: As ACC0, open any approved receipt. → As an accountant with Unlimited, open a large one.
  - Expected: ACC0 never sees Post; Unlimited always does.
  - Web error: Error: manual-match (setup): HTTP 400 — card_expense_transaction_already_matched {}

- **Seniors post at any amount @p0** — failed (account-hub/05-cards-posting.spec.js:181)
  - Where: /film-tools/account-hub/card-expenses · account: SEN
  - Steps: As SEN, open a receipt above every limit. → As PA (no Team row), same.
  - Expected: Post shown for both; no "Escalate" / "Submit for Review" buttons.
  - Web error: Error: manual-match (setup): HTTP 400 — card_expense_transaction_already_matched {}

- **Submit for Review → senior posts @p0** — failed (account-hub/05-cards-posting.spec.js:192)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100, "Submit for Review". → As SEN, "Process Card Expenses" → "Posting Review Queue". → Open it and post.
  - Expected: save-process with status: "under_review"; row in the senior tab with "Under Review" and "Review"; the tab is absent for non-seniors; the senior's Post sets posted.
  - Web error: Error: manual-match (setup): HTTP 400 — card_expense_transaction_already_matched {}

- **Escalate with a reason @p0** — failed (account-hub/05-cards-posting.spec.js:217)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100, "Escalate". → Try with no reason. → Type a reason, "Escalate". → As SEN, open it.
  - Expected: "Escalate to Senior" / "Escalating {receipt} to a senior accountant for review."; button disabled while blank; save-process {status: "escalated", escalation_reason}; senior sees "Escalated to Senior:" with the quote and an "Escalated" pill.
  - Web error: Error: manual-match (setup): HTTP 400 — card_expense_transaction_already_matched {}

### Top-ups

- **Crew requests a top-up @p0** — passed (account-hub/05-cards-posting.spec.js:239)
  - Where: /film-tools/account-hub/card-expenses · account: CREW
  - Steps: As CREW, "Card Extension" → "Request Top-up". → Amount 50, Reason "Fuel run". → "Send Request". → Click the row's "Show history".
  - Expected: "Send Request" disabled until amount > 0 and reason; POST /cards/:id/request-top-up {amount, reason} (no currency); list GET /cards/:id/top-ups shows the row as "Pending"; history drawer "Top-up History".

- **Post & Add Top-Up @p0** — failed (account-hub/05-cards-posting.spec.js:272)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Process a receipt uploaded with "Request top-up". → Read "Card Top-Up Decision". → Keep "Restore to Card Limit"; "Post & Add Top-Up". → Repeat with "No Top-Up".
  - Expected: Stats "Card Limit", "Card Balance", "This Expense", "Balance After"; options show "+£X top-up task" / "no change to balance"; post sends topUpMethod: "restore" and topUpAmount; a new row appears in Top-Up To Do. With No Top-Up the button reads "Post to Ledger" and no row is created.
  - Web error: Error: manual-match (setup): HTTP 400 — card_expense_transaction_already_matched {}

- **Mark a top-up done @p0** — failed (account-hub/05-cards-posting.spec.js:318)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Top-Up To Do". → On a pending row, "Mark Topped Up".
  - Expected: Rows sorted urgent first, then oldest; PATCH /topups/:id/complete; row moves to "Completed & Skipped"; card balance rises (limit unchanged). Filters "All / Pending / Completed / Partial / Skipped"; cash rows never shown; empty "No top-up tasks. Post receipts with top-up to create tasks here.".
  - Web error: Error: manual-match (setup): HTTP 400 — card_expense_transaction_already_matched {}

### Card requests & card holders

- **Crew requests a card @p0** — passed (account-hub/05-cards-access.spec.js:261)
  - Where: /film-tools/account-hub/card-expenses · account: CREW2
  - Steps: As CREW2, "Card" tab → "+ Request New Card". → Proposed Limit 400, keep the default Currency, Justification "Location fuel". → "Submit Request".
  - Expected: Card Holder and Department fields are read-only and prefilled; POST /cards with proposed_limit, justification, currency; modal closes; server success toast; a tile with badge "Requested" appears. Clean up: Delete the request.

- **Request cap (max amount) @p0** — passed (account-hub/05-cards-access.spec.js:294)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: With Request Cap on at 1,000, open "Request New Card". → Read the hint under Proposed Limit. → Enter 1,500 and submit.
  - Expected: Hint "Maximum £1,000.00 — project cap."; toast "Proposed limit exceeds the project cap of £1,000.00."; no request sent.

- **Accountant requests a card on behalf of crew @p0** — passed (account-hub/05-cards-access.spec.js:312)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Card Register → "Request New Card". → Card Holder (search), Card Provider (auto-selected when only one), Currency, Proposed Limit 300, BS Control Code, Justification. → "Submit Request".
  - Expected: POST /cards with user_id, card_provider_id, card_issuer, company_id, bs_control_code, currency; tile appears; no request-cap check on this form. Clean up: delete or reject.

- **No provider set up blocks the BS-code forms @p0** — skipped (account-hub/05-cards-access.spec.js:351)
  - Where: /film-tools/account-hub/card-expenses · account: SEN
  - Steps: Stub GET /settings with card_providers: []. → As SEN open "Request New Card". → As ACC100 open it.
  - Expected: Amber note under Card Provider: senior sees "No card provider is set up yet. Set one up before sending this card for approval." + "Set up provider"; non-senior sees "…Ask a senior accountant to add a card provider in Settings first." with no button; "Submit Request" disabled. A failed /settings (500) must not block.

### Card review, approval & activation

- **Requested card asks the accountant for review @p0** — passed (account-hub/05-cards-access.spec.js:383)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100, open a requested card tile.
  - Expected: Tile footer "Needs review before it can be submitted for approval."; detail shows "Action Needed" / "This card request needs to be reviewed before it can be sent for approval." with "Review"; header has no "Edit".

- **Review sends the card for approval @p0** — passed (account-hub/05-cards-access.spec.js:402)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Click "Review". → In "Edit Card Details" pick Card Provider, enter BS Control Code, limit and justification. → "Submit for Approval".
  - Expected: Picking a provider whose bank has another currency switches Currency to it (opening the form alone never changes it); PATCH /cards/:id with status: "pending", card_limit, recomputed balance, provider / bank / company, bs_control_code; badge "Pending (0/2)"; approval chain shows Level 1 "Awaiting approval".

- **Reject a card (reason required) @p0** — passed (account-hub/05-cards-access.spec.js:444)
  - Where: /film-tools/account-hub/card-expenses · account: APR1
  - Steps: As APR1, "Reject" on a pending card. → Try "Reject" with an empty reason. → Type a reason, "Reject".
  - Expected: Button disabled while the reason is blank; POST /cards/:id/reject with the reason; status rejected; tile shows "Rejection Reason" + text; detail shows "Rejected by {name}".

- **Requester fixes a rejected card @p0** — passed (account-hub/05-cards-access.spec.js:475)
  - Where: /film-tools/account-hub/card-expenses · account: requester
  - Steps: As the requester, Edit the rejected card. → "Re-submit". → As ACC100, open it.
  - Expected: Status requested again (approvals cleared); the accountant must "Review" before it re-enters the chain.

- **Level 1 approves a card @p0** — passed (account-hub/05-cards-access.spec.js:503)
  - Where: /film-tools/account-hub/card-expenses · account: APR1
  - Steps: As APR1, "Approval Queue" tab → "Cards". → Click "Approve" on the tile.
  - Expected: POST /cards/:id/approve {tier_number: 1, total_tiers: 2, user_id}; server toast; chain shows Level 1 with APR1's name and date; card waits for Level 2.

- **Override a card @p0** — passed (account-hub/05-cards-access.spec.js:531)
  - Where: /film-tools/account-hub/card-expenses · account: SEN
  - Steps: As SEN (not in the chain), open a pending card in Card Register. → "Override" → "Override Card Approval". → Leave the reason blank; "Override".
  - Expected: POST /cards/:id/override with reason "Overridden by accountant"; status override; accountant badge "Override", crew badge "In-Progress"; "Activate & Assign Card Number" offered.

- **Override visibility rules @p0** — skipped (account-hub/05-cards-access.spec.js:554)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Turn "Accountant Override — Card Requests" off; open a pending card as SEN. → Turn it on; open as ACC100 (no override right). → Open as OVR who is also the next approver.
  - Expected: No "Override" in steps 1–2; step 3 shows "Approve" / "Reject" instead of Override. The crew Approval Queue never shows Override.

- **Final level approves a card @p0** — passed (account-hub/05-cards-access.spec.js:580)
  - Where: /film-tools/account-hub/card-expenses · account: APR2
  - Steps: As APR2, approve the same card.
  - Expected: Status approved; tile badge "In-Progress"; accountant sees "Activate & Assign Card Number".

- **Activate a card (digital and physical) @p0** — passed (account-hub/05-cards-access.spec.js:603)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On an approved card, "Activate & Assign Card Number". → Card Type "Digital Card", type 16 digits. → "Activate Digital Card". → On another approved card choose "Physical Card" → "Activate Physical Card".
  - Expected: Counter "N digits remaining" counts down; digits grouped in fours; POST /cards/:id/activate {card_type, last_four, full_card_number, company_id}; digital badge "Digital Active" with "available of £X"; physical badge "Active".

- **One live card per person @p0** — passed (account-hub/05-cards-access.spec.js:662)
  - Where: /film-tools/account-hub/card-expenses · account: CREW
  - Steps: As CREW (holds an active card with balance > 0), open "Card". → Stub GET /cards?my=true with only a suspended card. → Stub one active card with balance 0.
  - Expected: Step 1: no "+ Request New Card". Steps 2 and 3: the button shows. Requested / pending / approved / override / rejected cards also hide it.

### Statements & transactions

- **Import a CSV statement @p0** — passed (account-hub/05-cards-receipts.spec.js:76)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Import Statement" → drop statement.csv. → In "Import statement" confirm Currency. → "Import statement".
  - Expected: Nothing uploads on drop; on confirm the file goes to S3 then POST /imports/import-statement {attachment, currency}; "Importing…" then tiles "Total Rows", "Imported", "Duplicates", "Declined"; every new row pre-selected.

- **Only CSV, OFX or QIF @p0 @smoke** — passed (account-hub/05-cards-receipts.spec.js:109)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Drag statement.pdf onto the drop zone. → Pick it through the file dialog.
  - Expected: Toast "Only CSV, OFX or QIF files can be imported"; no S3 upload and no import request either way.

- **Send statement lines to cardholders @p0** — passed (account-hub/05-cards-receipts.spec.js:135)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: After import, keep the selection. → "Submit N to Crew Portals".
  - Expected: POST /transactions/submit-to-users {transactionIds}; rows with a holder flip to "Pending Receipt"; panel "Submitted to Crew Portals", "Notified: {name} (n txn)"; "View Pending" opens Pending Coding; only new rows are selectable; "New Import" resets the page.

### Receipts (upload, match, missing receipts)

- **No active card blocks uploads @p0** — passed (account-hub/05-cards-receipts.spec.js:174)
  - Where: /film-tools/account-hub/card-expenses · account: CREW2
  - Steps: As CREW2 (no active card), open "My Transactions".
  - Expected: Notice "No active card" with "You don't have an active card right now…"; no "Upload Receipts" button.

- **Upload one receipt @p0** — passed (account-hub/05-cards-receipts.spec.js:187)
  - Where: /film-tools/account-hub/card-expenses · account: CREW
  - Steps: As CREW, "Upload Receipts". → Attach JPG, Date, Amount 20, "Merchant / Description", Category. → Optional "Budget Coding · optional" Cost Code. → "Submit 1 Receipt".
  - Expected: Full-page "Add Your Receipts" with "Card Limit / Committed / Available / N% used"; S3 upload then POST /receipts/submit-batch (each row: card_id, currency, company_id, cost_code, is_urgent, request_top_up, receipt_attachment); back on the list the new receipt shows its server status. Clean up: delete it.

- **Upload validation @p0** — passed (account-hub/05-cards-receipts.spec.js:218)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Submit Receipt 1 with no file. → Add file, no date. → No description. → Amount 0. 5. Date tomorrow.
  - Expected: Toasts in order: "Receipt 1: receipt attachment is required", "Receipt 1: date is required", "Receipt 1: description is required", "Receipt 1: amount must be greater than 0", "Receipt 1: date cannot be in the future"; no request.

- **Batch must fit the card headroom @p0** — passed (account-hub/05-cards-receipts.spec.js:249)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: With Available £100, enter receipts totalling £120. → Change to exactly £100.
  - Expected: Red "The total of these receipts exceeds your available card limit of £100.00." and submit disabled; exactly £100 is allowed.

- **Fill a statement line sent by the accountant @p0** — passed (account-hub/05-cards-receipts.spec.js:270)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: After AH-CARD-051, as the holder find the row "Upload your receipt for this transaction". → "Upload Receipt". → Save without a file, then with file, description and date: "Save Changes".
  - Expected: "Edit Receipt" marks Attachment, Merchant / Description and Date required; toast "Attachment is required" first; then PATCH /receipts/:id with card_id, currency, receipt_attachment.

### Assignment

- **Only the assignee or a senior opens a receipt @p0** — skipped (account-hub/05-cards-posting.spec.js:59)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100, view "Processing Queue". → As SEN, view it.
  - Expected: ACC100: own rows show "Me" and "Process"; others greyed with no button and clicking does nothing. SEN: every row opens, including "Unassigned".

### Posting & seniority / posting limits

- **Post a receipt @p0** — passed (account-hub/05-cards-posting.spec.js:81)
  - Where: /film-tools/account-hub/card-expenses · account: assignee
  - Steps: As the assignee with an unlimited limit, open an approved receipt. → Make sure every line has a nominal and an Effective Date is set. → "Post to Ledger".
  - Expected: Header badge "Ready to Post"; POST /receipts/:id/save-process with line_items (gross amounts), net_amount, tax_amount, gross_amount, effective_date, status: "posted", user_id; view closes; receipt appears in History as "Posted"; the crew card shows "Posted to ledger".

- **Coded total must match the receipt @p0** — passed (account-hub/05-cards-posting.spec.js:112)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Change a line so the coded gross differs by more than 1p. → Click "Save". → Click "Post to Ledger".
  - Expected: Banner "Line items total does not match receipt amount." (or the "lower than" variant); alerts "Coded lines must match the receipt amount before you can save" / "…before you can post"; nothing sent.

- **Every line needs a nominal to post @p0** — failed (account-hub/05-cards-posting.spec.js:133)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Clear a line's nominal. → "Post to Ledger". → "Save".
  - Expected: Post alert "Line 1 needs a nominal code before you can post" and the line turns red; Save still saves (save-process with no status).
  - Web error: Error: manual-match (setup): HTTP 400 — card_expense_transaction_already_matched {}

### Submit & approval levels (receipts)

- **Crew approver's receipts section @p0** — passed (account-hub/05-cards-receipts.spec.js:516)
  - Where: /film-tools/account-hub/card-expenses · account: APR1
  - Steps: As APR1, "Approval Queue" → "Receipts / Transactions". → "Approve" on a row. → Open another row: "Reject" / "Approve" in the footer.
  - Expected: GET /approvals; POST /approvals/:id/approve with tier_number = approvals+1; no Query and no Override. No client next-approver check: every listed row shows Approve.

- **Two-level receipt approval end to end @p0** — passed (account-hub/05-cards-receipts.spec.js:542)
  - Where: /film-tools/account-hub/card-expenses · account: assignee
  - Steps: APR1 approves. → APR2 approves. → As the assignee open "Process Card Expenses".
  - Expected: After step 1 the receipt is still awaiting (now for APR2); after step 2 it is approved and listed in "Processing Queue".

### Query / reject / resubmit

- **Reject a receipt (reason required) @p0** — failed (account-hub/05-cards-receipts.spec.js:574)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open a receipt as next approver → "Reject". → Try with an empty reason. → Enter a reason, "Reject".
  - Expected: "Reject Receipt" / "Reject {merchant}. Please provide a reason."; button disabled while blank; POST /approvals/:id/reject {reason, user_id}; crew sees "Rejected — review and resubmit".
  - Web error: Error: matching receipt [e2e] receipt IA26Q-21: manual-match HTTP 404 card_expense_transaction_not_found; receipt pending_receipt/unmatched txn null

- **Crew resubmits a rejected receipt @p0** — passed (account-hub/05-cards-receipts.spec.js:598)
  - Where: /film-tools/account-hub/card-expenses · account: holder
  - Steps: As the holder, click the rejected receipt. → Change the amount or file. → "Save & Resubmit".
  - Expected: Opens "Edit & Resubmit Receipt" directly, with "Rejection:" quote and "By: {name}"; PATCH /receipts/:id then POST /receipts/:id/confirm; status leaves rejected.

### Top-ups (possible defect)

- **Over-limit check with string balances @p1** — bug (account-hub/05-cards-posting.spec.js:360)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Stub GET /topups with card_balance: "450.00", card_limit: 500, amount 100. → "Mark Topped Up".
  - Expected: Expected: the explainer. Code adds card_balance + amount without parseFloat, so a string balance concatenates and the check can pass; confirm the wire type.
  - Web error: Error: expect(locator).toBeVisible() failed

### Settings & team rights

- **Add a team member @p0** — skipped (account-hub/05-cards-posting.spec.js:395)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Team & Posting Rights" → "Add Member". → User, Posting Limit 100, Can Override off. → "Add".
  - Expected: User list = accounts team minus existing members; saved immediately (PATCH /settings {team_members}); row shows "Above £100.00 Submit to Senior". 0 shows "No access Submit to Senior"; Unlimited shows "Unlimited".

- **Department coordinators @p0** — failed (account-hub/05-cards-posting.spec.js:419)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Add Coordinator". → Save with no department and no users. → Pick department, then users (only that department listed), toggle "Coding Required"; Save.
  - Expected: Errors "Select a department" / "Select at least one user"; then PATCH /settings {department_coordinators}; changing the department clears the users.
  - Web error: Test timeout of 360000ms exceeded.

- **Approval & Override switches take effect live @p0** — passed (account-hub/05-cards-posting.spec.js:459)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Toggle "Accountant Override — Card Requests", "— Receipts", "Require Coordinator Coding"; Save. → Watch an open Card Register / Approval Queue in another session.
  - Expected: PATCH /settings {approval_override}; the other session refetches /metadata on ah:card:settings and Override buttons / Coding Queue tab appear or disappear without reload.

### Access & navigation

- **Crew opens the module from the Film Tools tile @p0 @smoke** — passed (account-hub/06-petty-cash.spec.js:78)
  - Where: /film-tools/account-hub/cash-expenses · account: CREW
  - Steps: Sign in as CREW. → Film Tools → click the Petty Cash Expenses tile.
  - Expected: URL ends /cash-expenses/petty-cash/submit-claim. Header "Petty Cash Expenses" with eyebrow "Finance". Top tabs "Petty Cash" and "Out of Pocket"; sub-tabs "Float Request", "Submit Receipts", "Receipts History", "Cash Extension"; right tab "My Overview". No "Export", "Audit Queue", "History" or "Settings".

- **Accountant opens it from the Account Hub sidebar @p0 @smoke** — passed (account-hub/06-petty-cash.spec.js:100)
  - Where: /film-tools/account-hub/cash-expenses · account: ACCT_POSTER
  - Steps: Sign in as ACCT_POSTER. → Account Hub sidebar → "Petty Cash Expenses".
  - Expected: Lands on petty-cash/overview. Sub-tabs "Overview", "Post & Ledger", "Active Floats", "Top-Ups", "Cash Recon"; right tabs "Audit Queue", "Approval Queue", "History"; "Export" on the sub-tab bar; no crew tabs; no "Settings".

- **Accountant entering by the tile gets the crew view @p0 @smoke** — passed (account-hub/06-petty-cash.spec.js:119)
  - Where: /film-tools/account-hub/cash-expenses · account: ACCT_POSTER
  - Steps: Sign in as ACCT_POSTER. → Open the Petty Cash Expenses tile (?entry=tool).
  - Expected: Same tabs as AH-CASH-001; no Audit Queue, History, Settings or Export; no Account Hub sidebar; "Approval Queue" only if the user sits in an approval level.

- **Settings tab is PA/FC on hub entry only @p0 @smoke** — passed (account-hub/06-petty-cash.spec.js:135)
  - Where: /film-tools/account-hub/cash-expenses · account: SENIOR_PA
  - Steps: As SENIOR_PA open from the sidebar. → Reopen from the tile. → As TEAM_SENIOR open from the sidebar.
  - Expected: 1: right tab "Settings" shown. 2: hidden. 3: hidden (the team Is Senior flag alone doesn't grant Settings).

- **Sign-off tab needs the setting and a senior accountant @p0** — passed (account-hub/06-petty-cash.spec.js:147)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Settings → "Senior Sign-off Required" off → Save. → Check Petty Cash and Out of Pocket as SENIOR_PA. → Turn it on, Save; check SENIOR_PA, TEAM_SENIOR, ACCT_POSTER.
  - Expected: Off: no "Sign-off" sub-tab for anyone. On: "Sign-off" (Senior group) in both sections for SENIOR_PA and TEAM_SENIOR, never for ACCT_POSTER.

- **Coordinator tabs follow the coordinator row @p0** — passed (account-hub/06-petty-cash.spec.js:185)
  - Where: /film-tools/account-hub/cash-expenses · account: COORD
  - Steps: As COORD (Coding Required and View Floats on) open the tile. → Turn Coding Required off for the row, Save, reload COORD. → Turn View Floats off, reload.
  - Expected: 1: right tabs "Coding Queue", "Dept Overview", "My Overview"; Petty Cash shows "Active Floats" beside the crew tabs. 2: "Coding Queue" gone. 3: "Active Floats" gone. An accountant who is also a view-floats coordinator sees one "Active Floats" tab.

- **Approval Queue for non-accountant approvers only @p0 @smoke** — passed (account-hub/06-petty-cash.spec.js:215)
  - Where: /film-tools/account-hub/cash-expenses · account: APPROVER1
  - Steps: As APPROVER1 open the tile. → As CREW2 (no level) open the tile.
  - Expected: APPROVER1 sees "Approval Queue"; CREW2 doesn't.

- **A tab URL outside the role's set is bounced @p0 @smoke** — passed (account-hub/06-petty-cash.spec.js:229)
  - Where: /film-tools/account-hub/cash-expenses · account: CREW
  - Steps: As CREW open …/cash-expenses/audit-queue. → Repeat with /settings and /petty-cash/post.
  - Expected: Once metadata loads the URL is replaced with petty-cash/float-request (first visible tab); no accountant data renders.

- **Accountant "New Float" reaches the form @p0 @smoke** — passed (account-hub/06-petty-cash.spec.js:244)
  - Where: /film-tools/account-hub/cash-expenses · account: ACCT_POSTER
  - Steps: As ACCT_POSTER: Active Floats → "New Float". → Overview → Float Register → "New Float".
  - Expected: Lands on petty-cash/float-request with the form open directly; no bounce back to Overview.

- **No view right for the tool @p0 @smoke** — passed (account-hub/06-petty-cash.spec.js:263)
  - Where: /film-tools/account-hub/cash-expenses · account: NO_RIGHT
  - Steps: As NO_RIGHT open the tile URL.
  - Expected: Redirected (replace) to the Film Tools grid; the module never renders.

### Journey R — crew rejects an issued deal

- **Crew rejects an issued deal @p0** — skipped (account-hub/07-deal-memo-lifecycle.spec.js:79)
  - Where: 07 Deal Memo
  - Steps: "Reject". → Try "Confirm Rejection" with an empty reason. → Type a reason and confirm.
  - Expected: Modal "Reject deal memo" ("Tell your production administrator what you don't agree with…", "Reason for Rejection"). Confirm disabled until a reason. POST /deal/reject-crew {reason}; status "Rejected" for the crew and on All Deals.

- **Edit menu by status and role — rejected deal @p0** — skipped (account-hub/07-deal-memo-lifecycle.spec.js:108)
  - Where: 07 Deal Memo · account: ACC
  - Steps: As ACC open an active non-union deal, then an active union deal, then a rejected deal. → As POST open the active union deal.
  - Expected: ACC non-union active: menu "Update Non-Union Pay breakdown", "Update Nominals" (no "Edit Deal Memo"). ACC union active: single "Update Nominals". Rejected: only "Edit Deal Memo". POST union active: no Edit control at all.

### Access & navigation › as crew

- **View-only member lands on My Deal (approver flag off) @p0 @smoke** — passed (account-hub/07-deal-memo-access.spec.js:50)
  - Where: /film-tools/deal-memo · account: CREW
  - Steps: Sign in as CREW. → Open /film-tools/deal-memo.
  - Expected: URL becomes /my-deal. Tabs: "My Deal", "My NDA" only. No "Global Production Rates" button, no Export or Create.

- **No view access is bounced @p0 @smoke** — passed (account-hub/07-deal-memo-access.spec.js:62)
  - Where: /film-tools/deal-memo · account: NONE
  - Steps: Sign in as NONE. → Open /film-tools/deal-memo/deals.
  - Expected: Redirected to /film-tools. No deal-memo API call returns data to the page.

- **Posting-only paths closed to view-only @p0 @smoke** — passed (account-hub/07-deal-memo-access.spec.js:78)
  - Where: /film-tools/deal-memo · account: CREW
  - Steps: As CREW open each: /quick, /edit, /new, /setup-hub, /setup-hub/union/new, /templates/build, /first-setup, /global-production-rates, /nda/send, /nda-templates, /approvers.
  - Expected: Each redirects (replace) to /film-tools/deal-memo/my-deal.

### Access & navigation

- **Posting user lands on All Deals @p0 @smoke** — passed (account-hub/07-deal-memo-access.spec.js:34)
  - Where: /film-tools/deal-memo · account: POST
  - Steps: Sign in as POST. → Open /film-tools/deal-memo.
  - Expected: URL becomes /film-tools/deal-memo/deals. Tabs: "Overview", "All Deals", "My Deal", "NDAs", "Notices". "Global Production Rates" button at the right of the tab bar. No "My NDA" tab.

- **Accountant without posting grant still posts @p0 @smoke** — passed (account-hub/07-deal-memo-access.spec.js:96)
  - Where: /film-tools/deal-memo
  - Steps: Give ACC view_access but posting_access false. → Open the module.
  - Expected: Lands on All Deals with "Create Deal Memo", "Deal Memo Setup", "Export" visible.

- **Approval Queue only for approvers @p0** — passed (account-hub/07-deal-memo-access.spec.js:106)
  - Where: /film-tools/deal-memo · account: AP1
  - Steps: Sign in as AP1, open the module. → Repeat as CREW.
  - Expected: AP1 sees "Approval Queue" (and "NDAs" + "My NDA"). CREW sees neither "Approval Queue" nor "NDAs".

### Pending members (simulated: own member row patched to pending)

- **Pending member sees two tabs @p0 @smoke** — passed (account-hub/07-deal-memo-access.spec.js:146)
  - Where: /film-tools/deal-memo · account: PEND
  - Steps: Sign in as PEND. → Open /film-tools/deal-memo.
  - Expected: Header description "Your deal memo and NDA for this production — review them, complete your details, and sign." Tabs "My Deal" and "My NDA" only. The header Back button is shown.

- **Pending member deep links bounce @p0 @smoke** — passed (account-hub/07-deal-memo-access.spec.js:159)
  - Where: /film-tools/deal-memo · account: PEND
  - Steps: As PEND open /deals, /approval-queue, /quick.
  - Expected: All end on /my-deal.

- **Pending member completes and sends their deal @p0** — skipped (account-hub/07-deal-memo-access.spec.js:169)
  - Where: /film-tools/deal-memo · account: PEND
  - Steps: As PEND open My Deal. → "Complete your details", fill the required fields, "Save & Finish". → Sign "Deal Memo". → "Send for Approval".
  - Expected: Status badge becomes "Awaiting Approval". Same calls as CREW (PATCH /deal/crew-details, POST /deals/:id/deal-pdf/sign, POST /deals/:id/send-for-approval).

### Create a deal (sections)

- **Builder layout for a posting user @p0 @smoke** — passed (account-hub/07-deal-memo-builder.spec.js:59)
  - Where: 07 Deal Memo · account: ACC
  - Steps: As ACC: Create Deal Memo → Union.
  - Expected: Breadcrumb "Deal Memos · Draft". Buttons "Save as Draft" and "Issue Memo". Sections in order: Territory & Union (collapsed, no Edit), Crew Details, Employee Status, Deal Structure, Rates & Compensation, Allowances & Rentals, Crew Personal Details, Conditions & Documents, Nominal Coding, Payroll Start Form.

- **External crew member @p0** — passed (account-hub/07-deal-memo-builder.spec.js:86)
  - Where: 07 Deal Memo
  - Steps: Toggle "External crew member". → Type a "Crew Name". → Leave Email empty in Crew Personal Details and click "Issue Memo".
  - Expected: The picker becomes a free-text name. Issue lists Crew Personal Details — Email ("Crew contact required" wording on the per-section validator).

- **Deal type options @p0 @smoke** — passed (account-hub/07-deal-memo-builder.spec.js:106)
  - Where: 07 Deal Memo
  - Steps: Deal Structure → Edit → "Deal Type".
  - Expected: Options: Weekly Rolling, Fixed Term, Day Player, Buy-Out, Picture Deal, Box Rental Only. Start Date and Estimated End Date pre-fill from the production schedule; "Custom days" are copied from the production ("Reset to production days" restores them, and an unnamed custom day blocks Issue with "Custom day name").

- **Crew Personal Details validation @p0** — passed (account-hub/07-deal-memo-builder.spec.js:126)
  - Where: 07 Deal Memo
  - Steps: Leave "Full Legal Name" empty, enter Mobile "12". → Issue.
  - Expected: Lists Full Legal Name and the mobile error ("enter a valid phone number (5–15 digits)"). Note above the section: "Mandatory fields for the crew to enter are marked with an asterisk *, edit to change them."

### Rates & calculations

- **Scale rate resolves @p0** — skipped (account-hub/07-deal-memo-builder.spec.js:146)
  - Where: 07 Deal Memo
  - Steps: Union setup, pick a crew member with a rate-card designation. → Open Rates & Compensation.
  - Expected: GET /api/v2/deal-memo/designation-rates/resolve fires; "Day Rate (Agreed)" and "Weekly Rate" fill from the scale; "Union scale" shows Hourly / Daily / Weekly.

- **Day and weekly rates cross-fill @p0** — passed (account-hub/07-deal-memo-builder.spec.js:169)
  - Where: 07 Deal Memo
  - Steps: On an agreement with no daily/weekly work hours, type Day Rate 300. → Type Weekly Rate 2000. → Repeat on an agreement with weekly 48 h / daily 8 h.
  - Expected: Weekly becomes 1500.00 ("weekly auto-calculates (×5)"); typing 2000 weekly sets day 400.00 ("daily auto-calculates (÷5)"). On 48/8 the factor is 6: day 300 → weekly 1800.00.

- **Holiday pay inclusive vs exclusive @p0** — skipped (account-hub/07-deal-memo-builder.spec.js:189)
  - Where: 07 Deal Memo
  - Steps: Agreement whose HP fringe is 12.07%, Day Rate 300. → Choose "Inclusive (within rate)". → Choose "Exclusive (added on top)". → Clear the choice and issue.
  - Expected: Inclusive: "HP included within the agreed day rate (÷1.1207). Base rate: £267.69/day · HP element: £32.31/day". Exclusive: "HP will be added on top of the day rate at 12.07%. Total with HP: £336.21/day"; Rate Summary "Total Day Rate (with HP)" £336.21. No choice: blocked with "Holiday Pay (HP) Treatment" and the HP message. Picture and Buy-Out skip this check.

- **Enabled allowance rows must be complete @p0** — passed (account-hub/07-deal-memo-builder.spec.js:226)
  - Where: 07 Deal Memo
  - Steps: Allowances & Rentals → enable a row and clear its rate. → "Add Custom Rental", set "Capped" with no Cap Amount. → Issue. → Toggle the first row off and issue again.
  - Expected: Blocked: "Allowances & Rentals incomplete" with lines like "{name} — missing rate" and "{name} — missing cap_amount"; message "Allowances & Rentals: {names} — fill rate, pay frequency, applies-to (and cap amount when capped) on every enabled row, or toggle the row off." A toggled-off row is ignored. Missing nominal codes never block here.

### Autosave & drafts

- **First edit creates a draft @p0** — passed (account-hub/07-deal-memo-builder.spec.js:261)
  - Where: 07 Deal Memo
  - Steps: Open /quick, change one field. → Wait 6 s.
  - Expected: One POST /api/v2/deal-memo/deals?notify=false with a client-minted 24-hex _id. Indicator "Saving…" then "Saved {time}". All Deals → "Drafts" shows the row.

- **Leave Without Saving deletes this session's draft @p0** — bug (account-hub/07-deal-memo-builder.spec.js:288)
  - Where: 07 Deal Memo
  - Steps: New deal, edit, wait for autosave. → Back. → "Leave Without Saving".
  - Expected: Prompt "Save changes?" (or "Save as draft?" before the first autosave) with "You have unsaved changes to this deal memo…". DELETE /deals/:id for the autosaved draft; the row is gone from All Deals; the crew member is free again.
  - Web error: Error: expect(locator).toBeVisible() failed

### Validation & issue

- **Issue lists every failing section @p0 @smoke** — passed (account-hub/07-deal-memo-builder.spec.js:322)
  - Where: 07 Deal Memo
  - Steps: On an empty /quick click "Issue Memo".
  - Expected: Modal "Complete these sections before issuing" with "Each section below is highlighted on the page — open its Edit to fill what's missing." and one line per section with its fields; the first failing section opens in edit mode.

### Crew portal & NDA portal — crew deal (stubbed portal, real deal body)

- **Portal page content @p0 @smoke** — passed (account-hub/07-deal-memo-portal.spec.js:204)
  - Where: 07 Deal Memo
  - Steps: Verify as EXT.
  - Expected: "Crew Deal Memo", "Prepared for {name}", the memo, document chips, "Powered by ZILLIT · {year}". No app shell, no approval chain. GET /deal-memo/crew-portal/:token. The legacy path /crew-portal/:token opens the same gate and page.

- **Sign on the portal (stubbed sign) @p0 @smoke** — passed (account-hub/07-deal-memo-portal.spec.js:270)
  - Where: 07 Deal Memo
  - Steps: Click the "Deal Memo" chip. → Draw a signature ("Continue"), place it, "Sign Document".
  - Expected: Local draw pad (no saved-signature library). POST …/deal-pdf/sign/binary with Content-Type: application/pdf and X-Filename; the chip shows signed.

- **Sign on the portal (real) @p0** — skipped (account-hub/07-deal-memo-portal.spec.js:302)
  - Where: 07 Deal Memo
  - Steps: Click the "Deal Memo" chip. → Draw a signature ("Continue"), place it, "Sign Document".
  - Expected: Local draw pad (no saved-signature library). POST …/deal-pdf/sign/binary with Content-Type: application/pdf and X-Filename; the chip shows signed.

- **Send for approval from the portal (stubbed send) @p0 @smoke** — passed (account-hub/07-deal-memo-portal.spec.js:307)
  - Where: 07 Deal Memo
  - Steps: After signing, "Send for Approval".
  - Expected: POST …/send-for-approval; status "Awaiting Approval"; sign chips gone; the deal reaches AP1's queue.

- **Send for approval from the portal (real, reaches AP1) @p0** — skipped (account-hub/07-deal-memo-portal.spec.js:339)
  - Where: 07 Deal Memo
  - Steps: After signing, "Send for Approval".
  - Expected: POST …/send-for-approval; status "Awaiting Approval"; sign chips gone; the deal reaches AP1's queue.

### Crew portal & NDA portal — OTP gate

- **Gate holds the portal closed @p0 @smoke** — passed (account-hub/07-deal-memo-portal.spec.js:119)
  - Where: 07 Deal Memo
  - Steps: Open the crew link in a fresh context.
  - Expected: "STEP 1 OF 2", "Verify Your Identity", header "Deal Memo Portal", "This deal memo is protected. Enter the email address associated with the deal to receive a one-time code." No portal API call before the user acts.

- **Request a code (stubbed request-otp) @p0 @smoke** — passed (account-hub/07-deal-memo-portal.spec.js:140)
  - Where: 07 Deal Memo
  - Steps: Enter EXT's address, "Send Verification Code".
  - Expected: POST /api/v2/deal-memo/crew-portal/:token/request-otp. "STEP 2 OF 2", "Enter Verification Code", "We sent a 6-digit code to e****@domain. It expires in 15 minutes and can be used once." "Code expires in 14:5x"; "Resend in 0:30" then "Resend code".

- **Request a code (real email) @p0** — skipped (account-hub/07-deal-memo-portal.spec.js:161)
  - Where: 07 Deal Memo
  - Steps: Enter EXT's address, "Send Verification Code".
  - Expected: POST /api/v2/deal-memo/crew-portal/:token/request-otp. "STEP 2 OF 2", "Enter Verification Code", "We sent a 6-digit code to e****@domain. It expires in 15 minutes and can be used once." "Code expires in 14:5x"; "Resend in 0:30" then "Resend code".

- **Verify and enter (stubbed verify-otp) @p0 @smoke** — passed (account-hub/07-deal-memo-portal.spec.js:166)
  - Where: 07 Deal Memo
  - Steps: Paste the 6-digit code. → "Verify & Continue".
  - Expected: All six boxes fill from one paste. POST …/verify-otp; the portal loads with "You're viewing this deal memo as a verified recipient." and "Session expires in mm:ss".

- **Verify and enter (real code) @p0** — skipped (account-hub/07-deal-memo-portal.spec.js:195)
  - Where: 07 Deal Memo
  - Steps: Paste the 6-digit code. → "Verify & Continue".
  - Expected: All six boxes fill from one paste. POST …/verify-otp; the portal loads with "You're viewing this deal memo as a verified recipient." and "Session expires in mm:ss".

### Crew portal & NDA portal — crew deal (stubbed portal, real deal body)

- **Complete details on the portal (stubbed write) @p0** — passed (account-hub/07-deal-memo-portal.spec.js:232)
  - Where: 07 Deal Memo
  - Steps: "Complete your details". → Fill bank and emergency, save.
  - Expected: PATCH /deal-memo/crew-portal/:token/crew-details, then the portal refetches. Passport/ID upload is hidden; bank and loan-out fields stay editable.

- **Complete details on the portal (real) @p0** — skipped (account-hub/07-deal-memo-portal.spec.js:264)
  - Where: 07 Deal Memo
  - Steps: "Complete your details". → Fill bank and emergency, save.
  - Expected: PATCH /deal-memo/crew-portal/:token/crew-details, then the portal refetches. Passport/ID upload is hidden; bank and loan-out fields stay editable.

### Crew portal & NDA portal — NDA portal

- **NDA portal gate (stubbed OTP) @p0 @smoke** — passed (account-hub/07-deal-memo-portal.spec.js:399)
  - Where: 07 Deal Memo
  - Steps: Open EXT's NDA link in a fresh context. → Request and verify a code.
  - Expected: Header "NDA Portal", "This agreement is protected. Enter the email address it was sent to…". POST /deal-memo/nda-portal/:token/request-otp, …/verify-otp; then GET /deal-memo/nda-portal/:token. Header shows "Session expires in mm:ss".

- **NDA portal gate (real code) @p0** — skipped (account-hub/07-deal-memo-portal.spec.js:423)
  - Where: 07 Deal Memo
  - Steps: Open EXT's NDA link in a fresh context. → Request and verify a code.
  - Expected: Header "NDA Portal", "This agreement is protected. Enter the email address it was sent to…". POST /deal-memo/nda-portal/:token/request-otp, …/verify-otp; then GET /deal-memo/nda-portal/:token. Header shows "Session expires in mm:ss".

- **Sign the NDA (stubbed sign) @p0 @smoke** — passed (account-hub/07-deal-memo-portal.spec.js:428)
  - Where: 07 Deal Memo
  - Steps: Leave a field empty, "Sign NDA". → "Sign anyway", sign.
  - Expected: Warning "Sign with details still empty?". POST …/sign/binary; the view switches to the PDF; "Sign NDA" disappears; Download caption "Signed by you, not yet countersigned".

- **Sign the NDA (real) @p0** — skipped (account-hub/07-deal-memo-portal.spec.js:457)
  - Where: 07 Deal Memo
  - Steps: Leave a field empty, "Sign NDA". → "Sign anyway", sign.
  - Expected: Warning "Sign with details still empty?". POST …/sign/binary; the view switches to the PDF; "Sign NDA" disappears; Download caption "Signed by you, not yet countersigned".

### Create gate

- **No setups: pop-up instead of menu (setups list stubbed empty) @p0 @smoke** — passed (account-hub/07-deal-memo-setup.spec.js:47)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: On a project with no setups, as POST click "Create Deal Memo".
  - Expected: Modal "Set up Deal Memo first" with "You need to go to Deal Memo Setup to fill up Production Entity, select Union or Non-union and fill accordingly." Button "Open Deal Memo Setup" → /templates/build. No Union/Non-Union menu.

### Setup builder

- **Union setup minimum @p0** — passed (account-hub/07-deal-memo-setup.spec.js:123)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: New Union setup, leave Territory & Union empty. → "Save Setup".
  - Expected: Modal "Union Setup is incomplete" (or "Union Setup for {territory} is incomplete"): "A union setup needs a production entity, a territory and an agreement before it can be saved." Fields listed: Production Entity, Territory, Agreement. Nothing saved.

- **Save a Union setup @p0** — passed (account-hub/07-deal-memo-setup.spec.js:176)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Pick Production Entity, Territory, Agreement (and Budget Band if offered). → "Save Setup". → Enter a name (placeholder "e.g. UK Scripted — Camera HOD") and confirm.
  - Expected: POST /api/v2/deal-memo/templates (or PATCH of the autosaved draft) with the name; success toast; card appears in the Union group.

### Create gate

- **One setup is applied automatically @p0 @smoke** — passed (account-hub/07-deal-memo-setup.spec.js:68)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: With exactly one Union setup, "Create Deal Memo" → "Union".
  - Expected: /quick opens with "This deal starts from a Union setup"; the setup picker shows that setup; Territory & Union, rates, allowances and conditions are hydrated.

### Setup hub

- **Use a setup @p0 @smoke** — passed (account-hub/07-deal-memo-setup.spec.js:96)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: On a card click "Create Deal Memo". → Click the builder Back.
  - Expected: /quick opens hydrated from that setup with "This deal starts from a … setup". Back returns to /setup-hub/<group>.

### Setup builder

- **Non-Union setup minimum @p0** — passed (account-hub/07-deal-memo-setup.spec.js:148)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Non-Union → "New setup" on a project with no non-union rules. → "Save Setup".
  - Expected: Modal "Non-Union Setup is incomplete": "A non-union setup needs a production entity and pay rules with day types before it can be saved." Lists Production Entity and Non-Union Pay Rules.

### Payment runs

- **Payment Runs page @p0 @smoke** — passed (account-hub/04-invoices-payments.spec.js:87)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA › "Payment Runs".
  - Expected: Eyebrow "Pay", "Build payment runs from open items. BACs, wires, and cheques."; tabs "Open Items", "Wires", "Cheques", "Active Runs"; tiles incl. "BACs Queue", "Wires Pending"; GET /api/v2/invoices/payment-runs/open-items?method=all&page=1&per_page=25.

### Settings & team rights

- **Settings page @p0 @smoke** — passed (account-hub/04-invoices-payments.spec.js:271)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA › "Settings".
  - Expected: Eyebrow "Configure", title "Invoices Setup", "AP controls — posting limits, alert preferences, and the sign-off chain."; panels "Team & Posting Rights", "Alert Preferences", "Run Authorization", "Auto-Assignment Rules"; skeleton panels while GET /api/v2/invoices/settings loads.

### Access & navigation › as crew

- **Crew opens the same URL @p0 @smoke** — passed (account-hub/04-invoices.spec.js:114)
  - Where: /film-tools/account-hub/invoices · account: CREW-U
  - Steps: Sign in as CREW-U. → Go to /film-tools/account-hub/invoices.
  - Expected: Crew screen: heading "Invoices", "View and manage invoices.", tabs "Approval Queue" (selected), "My Department", "My Invoices", "Ongoing Uploads", button "Upload Invoices"; no accountant rail.

- **Crew reaches invoices from Purchase Orders @p0 @smoke** — passed (account-hub/04-invoices.spec.js:133)
  - Where: /film-tools/account-hub/invoices · account: CREW-U
  - Steps: As CREW-U open Purchase Orders. → Click the "Invoices" tab.
  - Expected: URL /film-tools/account-hub/purchase-orders/invoices; crew invoice tabs and "Upload Invoices" render inside PO without the "Invoices" page heading.

### Access & navigation

- **Accountant opens the module @p0 @smoke** — passed (account-hub/04-invoices.spec.js:100)
  - Where: /film-tools/account-hub/invoices · account: ACC-PA
  - Steps: Sign in as ACC-PA. → Go to /film-tools/account-hub/invoices.
  - Expected: URL becomes /invoices/overview; left rail titled "Invoices" with groups "Receive & Match", "Approve & Pay", "Vendors & Creditors", "Sales & Management"; the Account Hub sidebar is not shown.

- **Settings item only for seniors @p0 @smoke** — skipped (account-hub/04-invoices.spec.js:149)
  - Where: /film-tools/account-hub/invoices · account: ACC-PA
  - Steps: Sign in as ACC-PA, ACC-SR, then ACC-JR. → Look under "Sales & Management".
  - Expected: "Settings" shows for ACC-PA (designation) and ACC-SR (server senior), not for ACC-JR.

- **Non-senior deep link to Settings @p0 @smoke** — skipped (account-hub/04-invoices.spec.js:160)
  - Where: /film-tools/account-hub/invoices · account: ACC-JR
  - Steps: As ACC-JR open /film-tools/account-hub/invoices/settings.
  - Expected: Redirected to /invoices/overview; the "Invoices Setup" page never renders.

### Upload & inbox

- **Manual Entry required fields @p0 @smoke** — passed (account-hub/04-invoices.spec.js:299)
  - Where: /film-tools/account-hub/invoices
  - Steps: Enter Invoice › "Manual Entry". → Click "Create Invoice" empty. → Attach a .docx.
  - Expected: "Vendor is required", "Invoice number is required", "Invoice date is required", "Effective date is required", "Department is required", "Gross amount is required", "Attachment is required"; the .docx shows "Only PDF, JPG, or PNG files are allowed"; no POST /api/v2/invoices.

- **Inbox Queue list @p0** — passed (account-hub/04-invoices.spec.js:373)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA › "Invoice Inbox".
  - Expected: Eyebrow "Receive", title "Invoice Inbox", "Upload invoices or forward by email. AI extracts the data.", "{n} awaiting processing"; rows show invoice number (or "—"), vendor, description, gross, a PO pill (PO number or red "No PO"), status "OCR"/"New" and pay method; GET /api/v2/invoices/inbox?page=1&per_page=50.

### Receive & PO matching

- **Required fields on accept @p0** — passed (account-hub/04-invoices.spec.js:427)
  - Where: /film-tools/account-hub/invoices
  - Steps: Clear Vendor, Department and Effective Date; set Gross Amount 0. 2. "Accept and Match to PO".
  - Expected: Error list "Vendor is required", "Department is required", "Effective date is required", "Gross amount is required"; those fields outlined red; no POST /process.

- **Bulk Process blocked @p0** — passed (account-hub/04-invoices.spec.js:556)
  - Where: /film-tools/account-hub/invoices
  - Steps: Tick one complete row and one with no vendor. → "Process".
  - Expected: Dialog "Some invoices aren't ready": "1 of 2 selected invoice is missing required fields, so nothing was processed. Open each one to finish it." listing "{ref} — Vendor"; "Close"; clicking the ref opens its review; no /process.

### Upload & inbox

- **Manual Entry creates an inbox invoice @p0** — passed (account-hub/04-invoices.spec.js:325)
  - Where: /film-tools/account-hub/invoices
  - Steps: Fill Vendor, Invoice Number, Invoice Date, Effective Date, Department, Gross Amount, "PO Reference"; attach the 6-page PDF. → "Create Invoice".
  - Expected: POST /api/v2/invoices with status: "inbox" and po_number; modal closes; row in "Inbox Queue" with status "New"; the 6-page PDF is accepted (no page check here); the Effective Date picker won't allow a date on or before the CR lock.

### Receive & PO matching

- **Open an inbox invoice @p0** — failed (account-hub/04-invoices.spec.js:398)
  - Where: /film-tools/account-hub/invoices
  - Steps: Click an inbox row that has an unread chip.
  - Expected: "Invoice Review" modal: document on the left (PDF viewer or image), "Invoice Details" on the right, footer "Query", "Delete" (creator/assignee/senior) and "Accept and Match to PO"; GET /api/v2/invoices/:id; the row chip and the "Invoice Inbox" rail count drop.
  - Web error: Error: Timeout 30000ms exceeded while waiting on the predicate

- **Accept without a PO @p0** — failed (account-hub/04-invoices.spec.js:504)
  - Where: /film-tools/account-hub/invoices
  - Steps: Fill required fields, pick no PO, tick "No PO — verified, this invoice has no purchase order". → Accept. → "Continue".
  - Expected: Dialog "No PO Selected" / "No PO has been matched or selected for this invoice. Do you want to continue without a PO?" appears even with the box ticked; after Continue the invoice is in Pre-approval with "No PO".
  - Web error: Error: expect(locator).toContainText(expected) failed

- **Bulk Process @p0** — passed (account-hub/04-invoices.spec.js:527)
  - Where: /film-tools/account-hub/invoices
  - Steps: Tick 2 complete inbox rows. → Bar "2 invoices selected" › "Process".
  - Expected: POST /process { ids } with no po_ids and no updates; toast (server message, else "2 invoices moved to the register"); navigates to /invoices/register; both reach Pre-approval unmatched.

### Submit & approval levels › as crew approver

- **Last tier approves the invoice @p0** — skipped (account-hub/04-invoices-approval.spec.js:246)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-APR2 › "Approval Queue" tab › open the invoice › "Approve".
  - Expected: POST /:id/approve { tier_number: 2, total_tiers: 2 }; modal closes; the invoice leaves both queues and appears in "Invoice Entry".

- **Approval levels by amount and department @p0** — skipped (account-hub/04-invoices-approval.spec.js:273)
  - Where: /film-tools/account-hub/invoices
  - Steps: Send a 1,000 and a 6,000 D1 invoice and a D2 invoice to approval. → Check who sees Approve.
  - Expected: 1,000 → CREW-APR1 / ACC-APR; 6,000 → only CREW-APR3 at tier 1; the D2 invoice follows the "all" chain.

- **Crew approver approves in the modal @p0** — skipped (account-hub/04-invoices-approval.spec.js:303)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-APR1 › "Approval Queue" tab › open a pending BACS invoice.
  - Expected: Modal "Invoice {number}" with "Approval Chain" (current tier "Awaiting"); "Approve" and "Reject"; no "Query"; Approve → "Approving..." then closes and the tab refreshes.

### Submit & approval levels

- **Pre-approval list @p0** — passed (account-hub/04-invoices-approval.spec.js:110)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Invoices Pre-approval". → Search a vendor name.
  - Expected: Eyebrow "Match"; tiles "Matched" ("linked to a PO"), "Unmatched" ("need a PO match") and, when any, "On Hold" ("awaiting query"); panel "Invoices Pre-approval Queue" with "{n} invoices"; GET /api/v2/invoices/matching and GET /api/v2/invoices/held; search goes to the server for matching rows and filters held rows in the browser.

- **Row actions follow PO and rights @p0** — skipped (account-hub/04-invoices-approval.spec.js:141)
  - Where: /film-tools/account-hub/invoices · account: ACC-OVR
  - Steps: As ACC-OVR, then ACC-JR, view a matched row and a no-PO row.
  - Expected: Matched row: "Review" for both. No-PO row: ACC-OVR sees "Override", ACC-JR sees "Send for Approval". Pay method changes only the pill ("Urgent Wire Request", "Cheque Request", "No PO · …").

- **Review overlay @p0** — passed (account-hub/04-invoices-approval.spec.js:166)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Review" on a matched row. → Open "Confirm Match ▾".
  - Expected: Columns "Invoice PDF", "Purchase Order PDF", "Invoice Details" with "Linked POs (n)"; menu "Confirm & Send for Approval" ("Route through approval chain") and, only with override rights, "Confirm & Override"; also "Hold", "Query", "History".

- **Confirm & Send for Approval @p0** — passed (account-hub/04-invoices-approval.spec.js:182)
  - Where: /film-tools/account-hub/invoices
  - Steps: In the overlay choose "Confirm & Send for Approval".
  - Expected: POST /api/v2/invoices/:id/send-to-approval; overlay closes; row leaves Pre-approval; in "Approval Queue" it reads "Pending (0/2)" for the D1 chain.

- **Approval Queue list @p0** — passed (account-hub/04-invoices-approval.spec.js:199)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA › "Approval Queue".
  - Expected: Eyebrow "Approve", "Route invoices through the multi-tier approval chain.", pill "{n} awaiting"; status pills "Pending ({a}/{t})", "Rejected", "Approved"; SLA column = vendor payment terms or "—"; GET /api/v2/invoices/approval-queue?page=1&per_page=50.

- **Tier-1 approve by an accountant approver @p0** — skipped (account-hub/04-invoices-approval.spec.js:220)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-APR clicks the row's "Approve" on a 1,000 D1 invoice.
  - Expected: "Approving…"; POST /:id/approve { tier_number: 1, total_tiers: 2 }; pill "Pending (1/2)"; ACC-APR now sees "Chase" instead of Approve; CREW-APR2 can approve.

### Hold / override

- **Hold from the overlay @p0** — failed (account-hub/04-invoices-approval.spec.js:330)
  - Where: /film-tools/account-hub/invoices
  - Steps: Review a matched invoice › "Hold". → Try "Confirm Hold" with "Select reason...". → Pick "Other (specify in notes)" with empty "Query Notes". → Add notes › "Confirm Hold".
  - Expected: "Confirm Hold" disabled at steps 2–3; then "Holding…" and POST /:id/hold { holdReason: "Other (specify in notes)", notes }; overlay closes; banner "{ref} · {vendor} — ON HOLD" with "Reason: Other (specify in notes) · Notes: …".
  - Web error: Error: expect(locator).toBeVisible() failed

- **Release a hold @p0** — passed (account-hub/04-invoices-approval.spec.js:359)
  - Where: /film-tools/account-hub/invoices
  - Steps: Click "Release" on a held row (or its banner).
  - Expected: "Releasing…" then POST /:id/release; the invoice returns to the matching rows.

- **Override at Pre-approval @p0** — failed (account-hub/04-invoices-approval.spec.js:374)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-OVR clicks "Override" on a no-PO row. → In a matched overlay pick "Confirm & Override".
  - Expected: Each is one POST /api/v2/invoices/:id/override with {} (no status PATCH); invoices skip approval and show in "Invoice Entry" with status Override (assignee set if a rule matched).
  - Web error: Error: expect(locator).toContainText(expected) failed

- **Skip-approval Override in the queue @p0** — skipped (account-hub/04-invoices-approval.spec.js:405)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-OVR (not on the chain) and an approver with override rights view a pending BACS invoice (row and modal).
  - Expected: ACC-OVR sees "Override" and no Approve; the approver sees "Approve" and no Override; Override → /override → Entry.

- **Override & Pay on urgent pay methods @p0** — failed (account-hub/04-invoices-approval.spec.js:436)
  - Where: /film-tools/account-hub/invoices
  - Steps: Send a wire, a cheque and a faster-payment invoice to approval. → ACC-OVR (also an approver) opens the queue.
  - Expected: Rows show "Override & Pay" instead of Approve, pills "Urgent Wire Request", "Cheque Request", "Faster Payment"; click → "Processing…", /override, invoice in Entry (nothing is paid).
  - Web error: Error: faster pill

### Query / reject / resubmit › as crew approver

- **Reject with a reason @p0** — skipped (account-hub/04-invoices-approval.spec.js:466)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-APR1 opens a pending BACS invoice › "Reject". → Leave the reason empty. → Type a reason › "Reject Invoice".
  - Expected: Sub-modal "Reject {ref}" / "Please provide a reason for rejecting this invoice. This is mandatory."; button disabled and "Rejection reason is required" while empty; then POST /:id/reject { reason }; status "Rejected"; detail shows "Rejection Reason" with "By {name} · {date}".

### Query / reject / resubmit

- **Return to Approval from Entry @p0** — passed (account-hub/04-invoices-approval.spec.js:502)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open an approved invoice in Entry › "Return to Approval".
  - Expected: "Returning…" then POST /:id/return-to-approval; it is back in the Approval Queue as "Pending (0/n)" (approvals reset).

### Assignment

- **Non-senior row access @p0** — skipped (account-hub/04-invoices-entry.spec.js:93)
  - Where: /film-tools/account-hub/invoices
  - Steps: Assign Entry invoice A to ACC-JR and B to ACC-SR. → ACC-JR opens "Invoice Entry".
  - Expected: Both rows listed; B greyed with a lock and "—" instead of a checkbox, click does nothing; A opens; select-all ticks only A.

- **Senior row access @p0** — failed (account-hub/04-invoices-entry.spec.js:104)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-SR opens "Invoice Entry".
  - Expected: Every row opens and can be ticked; CR-locked rows have a disabled checkbox.
  - Web error: Error: expect(locator).toContainText(expected) failed

- **Bulk Assign @p0** — failed (account-hub/04-invoices-entry.spec.js:128)
  - Where: /film-tools/account-hub/invoices
  - Steps: Tick 2 rows › "Assign". → Pick "Assign to", leave the reason. → Pick "Other (custom reason)", type 501 chars. → Pick "Workload balancing" › "Assign".
  - Expected: Modal "Assign 2 invoices" with "Currently assigned to: …" and current assignees left out of "Select team member..."; "Assign" disabled until user + reason; custom text stops at 500 ("500/500"); then PATCH /:id { assigned_to, assignment_reason: "Workload Balancing" } per invoice; assignee column updates.
  - Web error: Test timeout of 90000ms exceeded.

### Line items & coding

- **Code a line @p0** — failed (account-hub/04-invoices-entry.spec.js:173)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Add another line"; fill Description, Code, Amount, tax type, Layers and Tags. → "Save".
  - Expected: Totals and the gross match bar update; PATCH /:id sends line_items with the wrapped nominal, tracking_codes (set id → code) and tags.
  - Web error: Test timeout of 90000ms exceeded.

- **Save @p0** — passed (account-hub/04-invoices-entry.spec.js:222)
  - Where: /film-tools/account-hub/invoices
  - Steps: Edit the invoice number and one line › "Save".
  - Expected: "Saving…" then PATCH /api/v2/invoices/:id with the full header (edited number included) and line_items; server message toast; screen stays open.

- **Save blocked by a mismatch @p0** — passed (account-hub/04-invoices-entry.spec.js:242)
  - Where: /film-tools/account-hub/invoices
  - Steps: Make the lines total differ from the invoice gross by more than 1p. → "Save".
  - Expected: Alert "Coded lines must match the invoice amount before you can save"; no PATCH.

### Validation

- **Post needs a vendor @p0** — passed (account-hub/04-invoices-entry.spec.js:256)
  - Where: /film-tools/account-hub/invoices
  - Steps: As ACC-PA clear Vendor › "Post to Ledger". → Type a new vendor name, pick create, Post.
  - Expected: "Select a vendor before posting to the ledger"; with a typed name the vendor is created only at Post and the post carries its id.

- **Post needs bank and effective date @p0** — passed (account-hub/04-invoices-entry.spec.js:313)
  - Where: /film-tools/account-hub/invoices
  - Steps: Clear Bank. → Pick a bank, clear Effective Date › Post.
  - Expected: No bank: "Post to Ledger" disabled with tooltip "Select a bank before posting to the ledger". No date: alert "Enter an effective date before posting to the ledger".

- **Post needs nominals and amounts @p0** — passed (account-hub/04-invoices-entry.spec.js:331)
  - Where: /film-tools/account-hub/invoices
  - Steps: Leave line 2 with no code › Post. → Give line 3 a description but no amount › Post.
  - Expected: "Line 2 needs a nominal code before you can post"; "Line 3 needs an amount before you can post"; nothing sent.

- **Post needs matching totals @p0** — passed (account-hub/04-invoices-entry.spec.js:356)
  - Where: /film-tools/account-hub/invoices
  - Steps: Lines total ≠ invoice gross › "Post to Ledger".
  - Expected: "Coded lines must match the invoice amount before you can post"; no request.

### Posting & seniority / posting limits

- **Who sees Post to Ledger @p0** — skipped (account-hub/04-invoices-entry.spec.js:372)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-POST (1,000), ACC-SR, ACC-PA (no team row), ACC-JR (0) and ACC-NONE each open an Entry invoice assigned to them.
  - Expected: "Post to Ledger" for ACC-POST, ACC-SR, ACC-PA; hidden for ACC-JR and ACC-NONE.

- **Post to Ledger @p0** — passed (account-hub/04-invoices-entry.spec.js:383)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA completes coding › "Post to Ledger".
  - Expected: "Posting…"; PATCH /:id then POST /:id/post; view closes; row leaves Entry; invoice shows "Ready to Pay" in "Posted Invoices" and in Payment Runs "Open Items".

- **Submit for Review @p0** — failed (account-hub/04-invoices-entry.spec.js:418)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-JR opens an assigned invoice › "Submit for Review". → In the list tick another › bar "Submit for Review". → ACC-SR opens Entry.
  - Expected: Detail sends PATCH /:id with header, lines and status: "under_review" (no amount, bank or nominal checks); bulk sends only { status: "under_review" }; rows show "Under Review" and match "Needs Review"; ACC-SR has no Submit for Review button.
  - Web error: TimeoutError: page.waitForResponse: Timeout 45000ms exceeded while waiting for event "response"

### Delete / void

- **Creator deletes at any status @p0** — passed (account-hub/04-invoices-entry.spec.js:436)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA opens an invoice it created and posted, in "Posted Invoices" › "Delete". → Confirm.
  - Expected: Dialog "Delete invoice?" / "{ref} from {vendor} will be permanently deleted. Its cost report and ledger entries will be removed too. This cannot be undone."; DELETE /api/v2/invoices/:id; back to the list without it. No void/undo exists.

### Payment runs › two run levels (level 2 = crew)

- **Run approval, first level @p0** — failed (account-hub/04-invoices-payments.spec.js:178)
  - Where: /film-tools/account-hub/invoices
  - Steps: Run Authorization: level 1 = ACC-RA; level 2 = ACC-RA and CREW-RA2. → ACC-RA opens the pending run › "Approve".
  - Expected: "Approving…"; POST /active-runs/:id/approve { tier_number: 1, total_tiers: 2 }; modal reloads; ACC-RA no longer sees Approve (already signed this run); CREW-RA2 now can.
  - Web error: Error: only this test's invoices are ticked before any run action

- **Crew Payment Run Approval tab @p0** — failed (account-hub/04-invoices-payments.spec.js:206)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-RA2 opens the tab with no runs, then with a run waiting at level 2.
  - Expected: Empty "No payment runs awaiting your approval"; list from GET /active-runs?status=pending&per_page=200 keeps only runs CREW-RA2 can sign now; run modal "Invoices in this run" with "Approve" / "Reject".
  - Web error: Error: only this test's invoices are ticked before any run action

### Access & navigation

- **Hub view right revoked mid-session @p0** — passed (account-hub/04-invoices.spec.js:168)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-JR sits on /invoices/inbox (hub entry). → Admin removes ACC-JR's view access to Account Hub.
  - Expected: ACC-JR is sent to /film-tools as soon as rights refresh, without a reload.

### Upload & inbox › as crew

- **Crew upload @p0** — passed (account-hub/04-invoices.spec.js:267)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-U › "Upload Invoices". → Add a PDF › "Upload & Submit (1)".
  - Expected: Modal "Upload Invoices" has no "Paid" switches; after submit the tab is "Ongoing Uploads" (?tab=uploads); the invoice later shows in CREW-U's "My Invoices" and in ACC-PA's Inbox.

### Upload & inbox

- **Upload one PDF (accountant) @p0** — passed (account-hub/04-invoices.spec.js:205)
  - Where: /film-tools/account-hub/invoices
  - Steps: Add a 1-page PDF. → Click "Upload & Submit (1)".
  - Expected: Lands on /invoices/inbox?tab=uploads; batch row reads "Uploading…" → "Extracting…" → "Upload complete" and clears itself about 6 s later; the invoice appears in "Inbox Queue" with status "OCR" (or "New"). Calls: S3 upload, POST /api/v2/invoices/bulk-upload, GET /api/v2/invoices/bulk-upload/batches.

- **Bulk upload of 3 mixed files @p0** — passed (account-hub/04-invoices.spec.js:249)
  - Where: /film-tools/account-hub/invoices
  - Steps: Add a PDF, a JPG and a PNG. → "Upload & Submit (3)".
  - Expected: Three POST /bulk-upload calls, one file each, all with the same batch_id; counters show progress; three new inbox rows.

### Receive & PO matching

- **Pick POs and read the match badge @p0** — passed (account-hub/04-invoices.spec.js:454)
  - Where: /film-tools/account-hub/invoices
  - Steps: Pick a PO in "PO Suggestions". → Add another via "All Purchase Orders" ("Search all POs..."). → Remove one chip.
  - Expected: GET /api/v2/invoices/:id/po-suggestions?vendor_id=…; chips show PO numbers; badge "Matched" (within 0.01), "Over PO +£x" or "Under PO −£x" with "PO total £y"; no badge if any selected PO amount is unknown.

- **Accept with a PO and match notes @p0** — passed (account-hub/04-invoices.spec.js:478)
  - Where: /film-tools/account-hub/invoices
  - Steps: Pick a PO; type "Match Notes (optional)". → "Accept and Match to PO".
  - Expected: POST /api/v2/invoices/process { ids:[id], updates:{ po_ids:[…], vendor_id, currency, … } }, then POST /api/v2/invoices/:id/match { po_id, notes:[text] } per PO; modal closes; invoice leaves the Inbox and shows in "Invoices Pre-approval" as "Matched".

### Access & navigation › as the crew account (P-VIEW)

- **Producer with view access, tool entry @p0 @smoke** — passed (account-hub/09-payroll-access.spec.js:71)
  - Where: /film-tools/account-hub/payroll · account: P-VIEW
  - Steps: Sign in as P-VIEW. → Film Tools → "Payroll". → Click "Producer Board Payroll Status". → Click the back chevron ("Back to Payroll").
  - Expected: Landing shows the two producer tiles; tile URL is /payroll/producer-board?entry=tool; Back returns to /film-tools/account-hub/payroll?entry=tool.

### Access & navigation

- **Accountant landing from Account Hub @p0 @smoke** — passed (account-hub/09-payroll-access.spec.js:43)
  - Where: /film-tools/account-hub/payroll · account: A-PLAIN
  - Steps: Sign in as A-PLAIN. → Account Hub sidebar → "Payroll".
  - Expected: URL /film-tools/account-hub/payroll; sidebar visible; eyebrow "Payroll Management", title "Payroll"; exactly 4 tiles: "Payroll Processing", "Payroll Run", "Payroll History", "Payroll Entry Setup". No "Producer Board Payroll Status" or "Production Report Payroll".

- **Accountant from the Film Tools tile sees producer tiles only @p0 @smoke** — passed (account-hub/09-payroll-access.spec.js:59)
  - Where: /film-tools/account-hub/payroll · account: A-PAFC
  - Steps: Sign in as A-PAFC. → Film Tools → "Payroll".
  - Expected: URL ends ?entry=tool; only "Producer Board Payroll Status" and "Production Report Payroll"; no "Payroll Entry Setup".

- **Payroll Entry Setup route @p0 @smoke** — passed (account-hub/09-payroll-access.spec.js:88)
  - Where: /film-tools/account-hub/payroll · account: A-PLAIN
  - Steps: As A-PLAIN, click "Payroll Entry Setup". → As P-VIEW, open /film-tools/account-hub/payroll/config.
  - Expected: Step 1: /payroll/config renders inside the Account Hub shell (sidebar visible), header "Payroll / Entry Setup". Step 2: redirected to /film-tools.

### Rights › as the crew account without payroll view (N-NONE)

- **No payroll_tool view access is sent away @p0 @smoke** — passed (account-hub/09-payroll-access.spec.js:118)
  - Where: /film-tools/account-hub/payroll · account: N-NONE
  - Steps: Sign in as N-NONE. → Open /film-tools/account-hub/payroll?entry=tool. → Open /payroll/run and /payroll/config.
  - Expected: Each redirects to /film-tools once rights have loaded; no payroll data flashes.

### Journal ledger

- **Open the ledger @p0 @smoke** — failed (account-hub/09-payroll-ledger.spec.js:65)
  - Where: /film-tools/account-hub/payroll
  - Steps: Run → "Journal Ledger".
  - Expected: Header "Payroll" / "Journal Ledger", chip "W/E {range}" (title "Processing week — a post pins to this week…"), "Effective date", "Post (N)", "Save" (disabled); back button "Back to Payroll Run"; columns Description, Code, Layers, Tags, Tax, Debit, Credit.
  - Web error: Error: expect(locator).toBeVisible() failed

### History

- **Open Payroll History @p0 @smoke** — passed (account-hub/09-payroll-ledger.spec.js:180)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open "Payroll History".
  - Expected: Last completed week; GET /api/v2/payroll/weekly/:ws/paid; tiles "Ready to post" (paid count) and "Posted"; list sorted by department then name.

### Processing (pull time cards, edit lines)

- **Opens on Outstanding @p0 @smoke** — passed (account-hub/09-payroll-processing.spec.js:78)
  - Where: /film-tools/account-hub/payroll · account: A-PLAIN
  - Steps: As A-PLAIN, open "Payroll Processing". → Switch to "Day View" and back to "Outstanding".
  - Expected: Default tab "Outstanding"; one GET /api/v2/payroll/timecards/weekly/payroll-processing/outstanding per visit (no repeat on tab switch); one row per crew member, largest gross first; paid rows hidden.

### Production-report estimate

- **Fill the whole week @p0 @smoke** — skipped (account-hub/09-payroll-producer.spec.js:31)
  - Where: /film-tools/account-hub/payroll
  - Steps: Select a crew member with no timecard. → "Fill from production report".
  - Expected: "Filling…"; GET /api/v2/ad-shoot-days/day-details?week_start=&user_id=<that crew>; toast "Filled N blank days from the Production Report."; days show estimated pay; no POST/PATCH.

### Producer Board

- **Read-only board @p0 @smoke** — passed (account-hub/09-payroll-producer.spec.js:100)
  - Where: /film-tools/account-hub/payroll · account: P-VIEW
  - Steps: As P-VIEW open "Producer Board Payroll Status". → Click another crew member. → "Previous week", then "Go to this week". → Stub an empty week.
  - Expected: Current period; GET /weekly/:ws/crew, first crew auto-selected, GET /timecards/weekly/:id; read-only week, no action buttons; "Next week" disabled on the current week. Empty: "No timecards yet" / "No timecards exist for this week yet.".

### Calculations, totals & currency

- **Run gross from a full timecard @p0 @smoke** — passed (account-hub/09-payroll-run.spec.js:61)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub GET /weekly/:ws/processing with one card: basic_pay 1000, overtime_pay 200, total_allowances 50, weekly_allowances [{ rate_amount 100, qty 1 }], one attached claim 30. 2. Open Run.
  - Expected: Row "Gross" £1,380.00; "+£30.00 claims" under it.

### Run & approval

- **Run opens on the last completed week @p0 @smoke** — passed (account-hub/09-payroll-run.spec.js:92)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open "Payroll Run". → Click "Next week".
  - Expected: Stepper "W/E {range}" for last week; "Go to this week" visible; after one step "Next week" is disabled and "Go to this week" hides.

### Entry setup / configuration

- **Page loads all six sections @p0 @smoke** — passed (account-hub/09-payroll-setup.spec.js:76)
  - Where: /film-tools/account-hub/payroll · account: A-PLAIN
  - Steps: As A-PLAIN, open /payroll/config.
  - Expected: GET /api/v2/account-hub/payroll-settings; hero "Configuration" / "Payroll Entry Setup" with tiles "Approvers" (count) and "Pay Window" (for example "Mon → Sun"); sections "SECTION 01" "Approvers" to "SECTION 06" "Payroll Accounts"; sidebar "Setup progress" with a counter.

- **Add and remove a final approver @p0** — failed (account-hub/09-payroll-setup.spec.js:101)
  - Where: /film-tools/account-hub/payroll
  - Steps: Under "Accounts team — available to add", click a user. → Click "Save & Apply". → Remove them with "Remove {name}". → Save.
  - Expected: Step 1: pill added, "Approvers · 1", section status "Set" (rendered uppercase). Step 2: PATCH /api/v2/account-hub/payroll-settings with payroll_approvers, success toast from the server, "All changes saved · HH:MM", then GET /api/v2/payroll/metadata. Step 3: "No approvers selected — pick from below.", status "Not set".
  - Web error: Error: expect(locator).toBeVisible() failed

- **Pay period, unlocked project @p0** — skipped (account-hub/09-payroll-setup.spec.js:162)
  - Where: /film-tools/account-hub/payroll
  - Steps: On the fresh project, set "Week starts on" = "Wednesday". → Set "Week ends on" = "Friday". → Save.
  - Expected: Step 1: end becomes "Tuesday", chip "Wed → Tue" (rendered uppercase). Step 2: start becomes "Saturday". Hint "You can change the start / end day freely until the first timecard is created…" shows. Save sends pay_period.

- **Pay period, locked project @p0** — passed (account-hub/09-payroll-setup.spec.js:192)
  - Where: /film-tools/account-hub/payroll
  - Steps: On a project with timecards, open "Pay Period". → Change "Approvers" and save.
  - Expected: Banner "Locked. Pay Period was frozen on {date} when the first timecard was created…"; both day selects disabled. The PATCH body has only payroll_approvers, journal_description_format, journal_group_by_category.

### Access & navigation — department users

- **Crew opens the tool from Film Tools @p0 @smoke** — passed (account-hub/03-purchase-orders.spec.js:107)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: Log in as U1. → Film Tools → click "Purchase Order".
  - Expected: URL /film-tools/account-hub/purchase-orders?entry=tool; title "Purchase Orders", description "Create, track, and manage purchase orders for your department."; tabs "Approval Queue", "My POs", "My Department POs", "Vendors", "Invoices"; right tabs "Templates", "PO Drafts", "Delivery Addresses"; button "Create PO"; no "All POs" tab; Approval Queue is active (GET /purchase-orders/approval).

- **Crew with posting right lands on All POs @p0 @smoke** — passed (account-hub/03-purchase-orders.spec.js:122)
  - Where: /film-tools/account-hub/purchase-orders · account: U2
  - Steps: Log in as U2. → Open the "Purchase Order" tile.
  - Expected: "All POs" is the first tab and active; GET /purchase-orders fires once (no Approval Queue fetch first); a brief "Loading purchase orders…" may show while rights load.

- **Bookmarked All POs URL without the right @p0 @smoke** — passed (account-hub/03-purchase-orders.spec.js:140)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: Log in as U1. → Go to /film-tools/account-hub/purchase-orders/all-pos.
  - Expected: Approval Queue content renders (GET /purchase-orders/approval); no "All POs" tab; no project-wide list is fetched.

- **Tool view right is enforced @p0 @smoke** — passed (account-hub/03-purchase-orders.spec.js:156)
  - Where: /film-tools/account-hub/purchase-orders · account: U7
  - Steps: Log in as U7. → Go to /film-tools/account-hub/purchase-orders?entry=tool.
  - Expected: Redirected to /film-tools once rights load. Via the hub (no entry=tool) the check is account_hub_tool instead.

### Access & navigation — accountant console

- **PA/FC console @p0 @smoke** — failed (account-hub/03-purchase-orders.spec.js:181)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: Log in as U6. → Open the Account Hub tile.
  - Expected: Tabs also show "Posted" and "Settings" (cog icon); no Assistant View banner; All POs table has a checkbox column.
  - Web error: Error: expect(locator).toBeVisible() failed

- **Accountant entering by the tile keeps the crew view @p0 @smoke** — passed (account-hub/03-purchase-orders.spec.js:201)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, click the "Purchase Order" tile. → Click "My POs", then "Templates".
  - Expected: Crew view throughout ("Create PO", "Approval Queue"); no console tabs even after the query string drops.

- **My Queue and All Queue @p0 @smoke** — passed (account-hub/03-purchase-orders.spec.js:228)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, open "Queue" → "My Queue". → Click "All Queue".
  - Expected: My Queue lists only POs whose assignee is U5, pending first; URL /queue/my. All Queue (/queue/all) lists pending, approved, acct-entered and rejected POs, U5's first; rows assigned to others (and not approvable by U5) are dimmed and do not open. Each sub-tab shows its count.

### Validation

- **At least one described line @p0 @smoke** — passed (account-hub/03-purchase-orders.spec.js:991)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open "Create PO", leave line descriptions empty. → Click "Create & Submit PO".
  - Expected: Error summary "Please fix the following errors" scrolled into view with "At least one line item with a description is required"; no POST.

### Access & navigation › crew

- **View right enforced @p0 @smoke** — passed (account-hub/03-asset-register.spec.js:98)
  - Where: /film-tools/asset-report · account: A4
  - Steps: Log in as A4. → Go to /film-tools/asset-report.
  - Expected: Redirected to /film-tools once rights load (no flash-redirect while they load).

### Access & navigation

- **Open from Film Tools @p0 @smoke** — passed (account-hub/03-asset-register.spec.js:84)
  - Where: /film-tools/asset-report · account: A1
  - Steps: As A1, Film Tools → "Asset Report". → Click the back arrow ("Back to Film Tools").
  - Expected: URL /film-tools/asset-report; title "Asset Register"; no hub sidebar; step 2 returns to /film-tools.

- **Department picker only for privileged users @p0 @smoke** — passed (account-hub/03-asset-register.spec.js:107)
  - Where: /film-tools/asset-report
  - Steps: Open the register as A1, then A2. → Open it as A3.
  - Expected: A1 and A2 see "Department" (placeholder "All departments"); A3 sees only "Currency", "Category" and search.

### Lists

- **Register loads @p0 @smoke** — failed (account-hub/03-asset-register.spec.js:134)
  - Where: /film-tools/asset-report · account: A1
  - Steps: Log in as A1. → Open the register.
  - Expected: Skeleton, then GET /purchase-orders/line-items and GET /vendors in parallel; columns "Code", "Asset", "Vendor", "Department", "Ref", "Exp. Type", "Qty", "Unit Cost", "Total"; header "<n> assets · <total>"; footer "Total". Ordered as the server sends (line total ascending).
  - Web error: Error: expect(locator).toBeVisible() failed

### Lifecycle B: reject, see the reason, resubmit, delete (two users)

- **Reject needs a reason @p0** — passed (account-hub/03-purchase-orders.spec.js:651)
  - Where: /film-tools/account-hub/purchase-orders · account: U3
  - Steps: As U3, open a pending PO → "Reject". → Try "Reject PO" empty. → Type a reason, click "Reject PO".
  - Expected: Modal "Reject PO", "Rejecting <PO>. Please provide a reason.", label "Reason *"; button disabled while empty; POST /purchase-orders/:id/reject { reason }; status "Rejected". Cancel/backdrop clears the text.

- **Creator sees the rejection @p0** — passed (account-hub/03-purchase-orders.spec.js:690)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, "My POs" → quick filter "Rejected". → Open the PO.
  - Expected: Row listed; detail shows "Rejection Reason" with the text and "— <name> — <designation>" of the rejector.

- **Resubmit a rejected PO @p0** — passed (account-hub/03-purchase-orders.spec.js:702)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, open the rejected PO → "Edit". → Fix it, click "Update & Submit PO".
  - Expected: PATCH /purchase-orders/:id with recomputed approvals and no status; after refetch the PO should be "Pending" again (backend resets — confirm).

- **Creator deletes a pending PO @p0** — passed (account-hub/03-purchase-orders.spec.js:727)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, open own pending PO → trash chip. → Confirm.
  - Expected: "Delete PO?" — "Are you sure you want to delete <PO>? This action cannot be undone."; "Deleting…"; DELETE /purchase-orders/:id; confirm and detail close only after success; list refreshes.

### Lifecycle C: accountant entry and Return to Approval

- **Accountant enters a PO @p0** — passed (account-hub/03-purchase-orders.spec.js:767)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, click "Enter PO". → Fill vendor, department, "Effective Date", one line. → Click "Create & Submit PO".
  - Expected: Topbar tag "Acct Entered", banner "Accountant PO Entry."; PO number shown as DRFT-000N (auto-generated); POST with status: "ACCT_ENTERED" and no approvals; lands on /purchase-orders/all; row status "Acct Entered".

- **Return to Approval @p0** — passed (account-hub/03-purchase-orders.spec.js:786)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5 (assignee), open an approved PO → "Return to Approval". → Confirm.
  - Expected: Confirm "Return to approval?" — "<PO> goes back to Pending. Its approvals are cleared and tier 1 approvers are notified again."; "Returning…" then POST /purchase-orders/:id/return-to-approval; PO "Pending"; detail closes (Process page left if open).

### Raise / create

- **Creator who is the tier-1 approver @p0** — passed (account-hub/03-purchase-orders.spec.js:809)
  - Where: /film-tools/account-hub/purchase-orders · account: U3
  - Steps: As U3, raise a Dept A PO of 100 and submit. → Open it as U3, then as U4.
  - Expected: POST body approvals holds { user_id: U3, tier_number: 1 }; status pill "Pending (1/2)"; U3 gets no Approve button on it; U4 does.

- **Picked department drives the chain @p0** — passed (account-hub/03-purchase-orders.spec.js:834)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1 (Dept A), click "Create PO" and pick department Dept B. → Submit, then open the PO in the console as U5.
  - Expected: Payload department_id = Dept B; the PO resolves Dept B's chain (no approver → plain "Pending" and the console Set Approver warning).

### Edit & drafts › crew drafts

- **Save as Draft @p0** — failed (account-hub/03-purchase-orders.spec.js:877)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, start a PO with only a vendor. → Click "Save" ▾ → "Save as Draft".
  - Expected: No validation runs; POST /purchase-orders with status: "DRAFT"; lands on /purchase-orders/drafts; row status "Draft" with "Draft Number"; the draft is absent from every PO list.
  - Web error: TimeoutError: page.waitForRequest: Timeout 60000ms exceeded while waiting for event "request"

- **Resume and submit a draft (crew) @p0** — failed (account-hub/03-purchase-orders.spec.js:909)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: In "PO Drafts" click the draft. → Complete it. → Click "Create & Submit PO".
  - Expected: "Loading draft…", GET /purchase-orders/:id; form crumb "Edit Draft" with tag "Draft"; submit sends PATCH /purchase-orders/:id with status: "PENDING"; draft leaves PO Drafts.
  - Web error: TimeoutError: page.waitForRequest: Timeout 60000ms exceeded while waiting for event "request"

### Edit & drafts › creator edits

- **Creator edits a pending PO @p0** — failed (account-hub/03-purchase-orders.spec.js:959)
  - Where: /film-tools/account-hub/purchase-orders · account: U3
  - Steps: As U3, open own pending PO with tier 1 auto-approved. → Click "Edit (resets approvals)". → Change the description, click "Update & Submit PO".
  - Expected: Edit chip shows "(resets approvals)" when approvals exist; PATCH /purchase-orders/:id with recomputed approvals and no status; list refreshes silently.
  - Web error: Error: expect(locator).toContainText(expected) failed

### Submit & approval levels

- **"Amount greater than" replaces the default users @p0** — failed (account-hub/03-purchase-orders.spec.js:1012)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U1 submits Dept A PO of 1500 (threshold 1000). → Open as U3, then as U6. → Repeat with 500.
  - Expected: 1500: tier 1 resolves to the conditional users only — U6 sees "Approve", U3 does not. 500: default user U3 approves tier 1. Tier count and "(n/m)" follow the resolved chain.
  - Web error: Error: expect(locator).toBeVisible() failed

### Bank Reconciliation — access & navigation

- **Department user can reach the module by URL @p1** — bug (account-hub/10-bank-reconciliation.spec.js:95)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: As DEPT visit /film-tools/account-hub/bank-reconciliation/overview.
  - Expected: Expected: redirect (accountant-only). Code: the module renders with every action available.
  - Web error: Error: expect(page).toHaveURL(expected) failed

### Bank Reconciliation — statement to sign-off

- **Sign off a clean period @p0** — failed (account-hub/10-bank-reconciliation.spec.js:199)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Fully match a period (difference 0). → "Sign Off". → Click "Sign Off" in the modal.
  - Expected: Modal "Sign Off Reconciliation", "Ready to sign off" / "All transactions matched, no outstanding items", note optional; POST /periods/<id>/sign-off {sign_off_notes}; period becomes Complete; Overview shows "View" (not Open) and no delete.
  - Web error: Error: expect(locator).toBeVisible() failed

- **Import a one-month statement @p0** — passed (account-hub/10-bank-reconciliation.spec.js:247)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Pick the GBP account, attach the one-month CSV. → Click "Import & Auto-Match". → Click "Done".
  - Expected: S3 upload then POST /api/v2/bank-reconciliations/import-statement {attachment, bank_account_id}; progress steps, then "<account> statement", "<n> transactions" with Matched / Suggested / Unmatched / Fraud Flags counts. Done refetches periods, exceptions, fraud alerts, FX variances; a new In Progress period appears.

- **KPI row @p0 @smoke** — passed (account-hub/10-bank-reconciliation.spec.js:262)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: With one in-progress period open Overview.
  - Expected: KPIs "Bank Balance" (Statement closing), "Zillit Balance" (Ledger balance), "Difference" (To reconcile), "Period", for the most recently created in-progress period.

- **Open the Workspace for a period @p0 @smoke** — passed (account-hub/10-bank-reconciliation.spec.js:328)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Overview › "Open" on an in-progress row. → Click a matched bank line.
  - Expected: URL workspace?period=<id>, expanded full screen ("Accounting · Bank reconciliation"); left "Bank Statement", right "Zillit Ledger" / "Ready to Pay · <period>"; GET /workspace-data?period_id=<id>; clicking a matched line highlights its ledger pair.

- **Generate modal validation @p0 @smoke** — passed (account-hub/10-bank-reconciliation.spec.js:343)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Generate & Share Link". → Leave fields empty, then fill Recipient Name, Email Address, Organisation Type, Period, untick all permissions.
  - Expected: Modal "Generate Guarantor / Broadcaster Portal Link"; Bank Account shown only with >1 account ("All accounts"); "What can they see?" six options (four on by default); Link Expiry 7 days / 14 days / 30 days / No expiry; "Notify me when viewed" (No notifications default); "Generate Link & Send Email" disabled until name, email, type, a listed period and ≥1 permission.

- **Accept a suggested match @p0** — skipped (account-hub/10-bank-reconciliation.spec.js:383)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: On an amber "AI Match: … · <pct>% confidence" bar click "Accept". → Click "Confirm Match".
  - Expected: Modal shows both sides and "This will match the bank transaction to the invoice and mark both as reconciled."; POST /api/v2/bank-reconciliations/<txnId>/match {matched_to_id, matched_to_type:"invoice"}; both rows turn matched.

- **Accept on a fraud-flagged line @p0** — passed (account-hub/10-bank-reconciliation.spec.js:402)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: On a red fraud row's suggested bar click "Accept". → Click "Accept & Match".
  - Expected: Danger modal "Fraud Alert — Confirm Match", "<type> — Risk Score: <n>", audit warning; match call as above; row shows the green "Fraud … & matched … Recorded in audit log." bar; Audit Log gains "Accepted & Matched".

- **Manual Match @p0** — failed (account-hub/10-bank-reconciliation.spec.js:423)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Manual Match" on an unmatched line. → Search 1250, then zzz. → Pick an entry, "Back", pick again, "Confirm Match".
  - Expected: Overlay "Select a ledger entry to match (<n>)" with search "Search by supplier, ref or amount…"; 1250 finds -£1,250.00; zzz shows "No ledger entries match “zzz”"; query survives Back; POST /<txnId>/match {matched_to_id, matched_to_type}.
  - Web error: Error: expect(locator).toBeVisible() failed

- **Quick Add on an unmatched line @p0** — failed (account-hub/10-bank-reconciliation.spec.js:456)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Blue "Not in Zillit — add or match to ledger?" bar › "Quick Add". → Fill Amount, Tax, Nominal Code, Cost Centre. → "Add & Match".
  - Expected: Quick Entry opens filled (date, description, amount), row shows "Quick Adding"; Add & Match disabled until a line and amount; POST /exceptions/<exception_id>/quick-add {amount, description, effectiveDate, vatType, vatRate, nominal, costCentre}; line turns matched and the workspace refreshes.
  - Web error: Error: expect(locator).toBeVisible() failed

- **Unreconcile one and many @p0** — skipped (account-hub/10-bank-reconciliation.spec.js:522)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click "Unreconcile" in the bank strip. → "Unreconcile ›" on one row → confirm "Unreconcile". → Tick two rows → "Unreconcile (2)" → confirm. → "Done".
  - Expected: Mode shows matched rows only with pills disabled; confirm "Unreconcile transaction?" / "Unreconcile 2 transactions?"; POST /<txnId>/unmatch then POST /bulk-unmatch {transaction_ids}; rows return to unmatched; Done restores the filter. Toggle hidden when nothing is matched.

- **Sign off with outstanding items @p0** — passed (account-hub/10-bank-reconciliation.spec.js:558)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Leave an unmatched line and a fraud flag. → "Sign Off". → Try without a note, then add one.
  - Expected: Warnings (fraud flags, items not posted, "Unreconciled Difference" "Bank total … vs Zillit total …"); note "(required)"; button "Sign Off with Exceptions" disabled until a note is typed; sign-off is never blocked otherwise.

- **Workspace after sign-off @p1** — failed (account-hub/10-bank-reconciliation.spec.js:597)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Sign off from the Workspace. → Look at the toolbar and rows.
  - Expected: Expected the page leaves the period. Code keeps showing it with "Sign Off", Accept, Manual Match and Add & Match enabled (only Unreconcile blocked) (K).
  - Web error: Error: Sign Off 1, action buttons 22

- **Sign-off marks matched invoices paid @p0** — failed (account-hub/10-bank-reconciliation.spec.js:605)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Note an invoice matched in the period. → Sign off. → Open Invoices.
  - Expected: Invoice list refreshes (br:period:signed-off → ah:invoice:list) and the matched invoice shows as paid.
  - Web error: Error: an open invoices list refetched after the sign-off

- **Delete one open period @p0** — passed (account-hub/10-bank-reconciliation.spec.js:620)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Bin icon on an open period. → Read the confirm, click "Cancel". → Repeat, click "Delete period".
  - Expected: Confirm "Delete this period?" naming the month and listing what is removed; Cancel sends nothing; Delete → POST /periods/bulk-delete {period_ids:[id]}; list refreshes; matched invoices return to unmatched. A Period Details modal open in another tab closes with "This period was deleted by another user".

- **Post one FX variance @p0** — skipped (account-hub/10-bank-reconciliation.spec.js:482)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Post" on a row. → Check Budget Rate (locked when from Production Setup). → On a row with no rate type both rates → "Confirm & Post".
  - Expected: Modal "Post FX Variance"; "Confirm & Post" disabled until both rates ("Enter both rates to post"); POST /fx-variances/<id>/post {nominal_code, cost_centre, budget_rate, bank_rate}; "Posted successfully"; modal survives a list refetch underneath.

- **cleanup — delete this run's open periods** — passed (account-hub/10-bank-reconciliation.spec.js:597)

### Guarantor portal — public page

- **Terminal link states (stubbed) @p0 @smoke** — passed (account-hub/10-bank-reconciliation.spec.js:719)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Route request-otp → br_portal_link_revoked, br_portal_link_expired, br_portal_link_not_found_or_revoked.
  - Expected: "Link revoked." / "Link expired." / "Link unavailable." with no Send or Resend. Real check: revoke a link (AH-BR-071) and open it.

### Bank Reconciliation — statement to sign-off

- **cleanup — delete this run's open periods and unpaid invoices** — passed (account-hub/10-bank-reconciliation.spec.js:662)

### Bank Reconciliation — access & navigation

- **Accountant opens Bank Reconciliation @p0 @smoke** — passed (account-hub/10-bank-reconciliation.spec.js:85)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: As ACC click "Management" › "Bank Reconciliation".
  - Expected: URL ends /bank-reconciliation/overview; header eyebrow "Management", title "Bank Reconciliation", "Match bank statements to ledger entries and manage reconciliation periods."; tabs Overview, Workspace, Exceptions, Fraud Alerts, FX Variances, History, Open Banking, Guarantor Portal, Accounts & Rules.

### Bank Reconciliation — import modal

- **Import modal gating @p0 @smoke** — passed (account-hub/10-bank-reconciliation.spec.js:112)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click "Import Statement". → Before choosing an account look for the drop zone. → Choose an account.
  - Expected: Modal "Import Bank Statement", "Select Bank Account" lists production bank accounts; drop zone ("Drop … here", "or click to browse", "Supported formats: CSV, OFX, QIF, MT940, PDF") appears only after an account is picked; "Import & Auto-Match" disabled until file + account.

### Guarantor portal — public page

- **Public page step 1 @p0 @smoke** — passed (account-hub/10-bank-reconciliation.spec.js:575)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: In a logged-out context open /guarantor-portal/<token>.
  - Expected: No app chrome or login redirect; "Bank Reconciliation Portal", "Secure Access", "STEP 1 OF 2", "Verify Your Identity", intro text, "Email Address"; "Send Verification Code" disabled until a valid email; no portal data request yet.

- **Request a code (stubbed request-otp) @p0 @smoke** — passed (account-hub/10-bank-reconciliation.spec.js:599)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Enter the recipient email. → "Send Verification Code".
  - Expected: POST …/bank-reconciliations/portal/<token>/request-otp {email}; "STEP 2 OF 2", "Enter Verification Code", "We sent a 6-digit code to j****@…"; "Code expires in m:ss"; "Resend in 0:30" until the cooldown ends.

- **Request a code (real email) @p0** — skipped (account-hub/10-bank-reconciliation.spec.js:621)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Enter the recipient email. → "Send Verification Code".
  - Expected: POST …/bank-reconciliations/portal/<token>/request-otp {email}; "STEP 2 OF 2", "Enter Verification Code", "We sent a 6-digit code to j****@…"; "Code expires in m:ss"; "Resend in 0:30" until the cooldown ends.

- **Verify and view read-only (stubbed verify + view) @p0 @smoke** — passed (account-hub/10-bank-reconciliation.spec.js:626)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Type the 6-digit code. → "Verify & Continue".
  - Expected: POST …/verify-otp; session saved in sessionStorage (br_portal_session…); GET …/bank-reconciliations/portal/<token> with the bearer; "Bank Reconciliation Summary", "Read-only"; sections without permission blurred "Not included"; footer "Powered by ZILLIT · This is a read-only view · <year>".

- **Verify and view read-only (real code) @p0** — skipped (account-hub/10-bank-reconciliation.spec.js:657)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Type the 6-digit code. → "Verify & Continue".
  - Expected: POST …/verify-otp; session saved in sessionStorage (br_portal_session…); GET …/bank-reconciliations/portal/<token> with the bearer; "Bank Reconciliation Summary", "Read-only"; sections without permission blurred "Not included"; footer "Powered by ZILLIT · This is a read-only view · <year>".

### Guarantor portal — links

- **Generate and email a link @p0** — skipped (account-hub/10-bank-reconciliation.spec.js:690)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Fill the modal for a test inbox, Fraud Alerts off. → Click "Generate Link & Send Email".
  - Expected: POST /portal-links with the form; success toast; row "Active" with permission chips, Expires, Views 0; email with the link arrives.

- **Revoke @p0** — skipped (account-hub/10-bank-reconciliation.spec.js:695)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Revoke" on an active link.
  - Expected: No confirm; "Revoking..." then PATCH /portal-links/<id>/revoke; status "Revoked"; actions become "Re-share".

### Cost Report — figures

- **Posted invoice lands in Actuals @p0** — passed (account-hub/10-cost-report.spec.js:304)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Note Actuals This Period / Actuals to Date for code X. → Post an invoice for X, amount A, dated this week (Invoices module). → Refresh the worksheet (pill or reload).
  - Expected: Both Actuals columns for X rise by A (in the display currency); header and Grand Total rise by A; the ledger drawer for X lists the invoice under "Actuals" (type INV).

- **Approved PO lands in PO Commits @p0** — passed (account-hub/10-cost-report.spec.js:343)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Note PO Commits for code Y. → Fully approve/post a PO line on Y for amount B. → Refresh.
  - Expected: PO Commits for Y rises by B; drawer "Commits · PO" lists the PO; EFC rises only if B pushes ATD + commits over budget (default ETC absorbs it).

### Cost Report — editing & versions

- **Save Version @p0** — passed (account-hub/10-cost-report.spec.js:417)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Edit two ETC cells. → Click "Save Version", label E2E-<id>, click "Save Version".
  - Expected: POST /api/v2/cost-reports/weekly-etc/versions with week_ending, label, one line per overridden account {account, etc_amount, vtp_amount} and currency; the new version is selected in "Version" and Compute stays hidden.

- **Changing week keeps the old version selected @p1** — failed (account-hub/10-cost-report.spec.js:456)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Select version V in Wk N. → Click "Next week". → Click "Save".
  - Expected: Version shows "— Latest (unsaved) —" but Save stays enabled and sends PATCH /weekly-etc/versions/V, overwriting Wk N's version with Wk N+1's overrides.
  - Web error: Error: expect(locator).toBeDisabled() failed

### Cost Report — period lock, close and publish

- **Publish Daily @p0** — failed (account-hub/10-cost-report.spec.js:492)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click "Publish". → Cadence "Daily", note E2E-<id>. → Click "Publish CR".
  - Expected: Modal "Publish Cost Report" closes at once; button reads "Publishing…"; POST /snapshots {cadence:"daily", post_note, company_id?, budget_version_id?, currency, etc_version_id?}; toast "Posting Daily CR…" → "Posted: <reference>"; CR History reloads with a "Daily" row.
  - Web error: Test timeout of 90000ms exceeded.

- **Lock the week @p0** — failed (account-hub/10-cost-report.spec.js:549)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open the Lock modal, add note E2E-<id>. → Click "Lock".
  - Expected: POST /lock-period {as_of: week end}, POST /snapshots {cadence:"adhoc", name:"Period Lock — Wk N…", period_start, period_end, budget_version_id, currency, post_note} with no company_id/etc_version_id, then GET /lock-period. Progress toast "Locking …" → "Locked …" / "Snapshot posted to history". Button becomes "Wk N … locked" (disabled); CR History gains a "Lock" row.
  - Web error: Error: expect(locator).toBeDisabled() failed

- **Lock freezes source documents in other modules @p0** — passed (account-hub/10-cost-report.spec.js:611)
  - Where: /film-tools/account-hub/cost-report
  - Steps: After locking through date D, open new PO, invoice, card receipt and cash claim forms. → Open an existing document dated ≤ D.
  - Expected: Effective-date pickers have min D+1 and default max(today, D+1); the old document shows the period-lock banner and is read-only.

- **Close Period @p0** — passed (account-hub/10-cost-report.spec.js:642)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Pick a date after the lock. → Click "Close Period".
  - Expected: POST /lock-period {as_of: noon UTC of that day}; message "Period closed. Locked through <date>."; Last closed period updates; no snapshot posted (CR History unchanged).

- **/lock-period failure shows weeks unlocked @p1** — failed (account-hub/10-cost-report.spec.js:675)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Project already locked. → Route GET /lock-period → 500. → Open a locked week.
  - Expected: Expected: locked (settings fallback). Code: cells editable and Lock enabled, because refreshLockState has no settings fallback (Period Close and other modules still show the lock).
  - Web error: Error: expect(locator).toBeDisabled() failed

### Cost Report — export

- **Worksheet export @p0** — failed (account-hub/10-cost-report.spec.js:767)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Edit one ETC. → "Export" → choose "Full Cost Report", "Working Report" and "Raw Export" in turn → "Export & Download".
  - Expected: Modal "Export Cost Report" with Status pill "Locked" / "Open" and "Generated by"; POST /api/v2/cost-reports/export/{pdf,xlsx,csv} with applied week, budget_version_id, currency, generated_by ("Name · Designation"), etc_overrides/efc_overrides/vtp_overrides; toast "Generating Cost Report" → "File Generated successfully"; file downloads.
  - Web error: Error: expect(locator).toBeVisible() failed

### Cost Report tool

- **Open a posted snapshot @p0 @smoke** — failed (account-hub/10-cost-report.spec.js:960)
  - Where: /film-tools/cost-report
  - Steps: Click a Posted CRs row.
  - Expected: /film-tools/cost-report/posted/:id; title paints from the preview ("<Cadence> CR — <name>", or red "Period Lock — …"), italic post note; Details (Cadence, Period, Posted, Posted by, Status "Posted to ledger", Reference); KPIs (Total Budget, Actuals to Date, Total Commitments, Est. Final Cost, Posted Variance); table; footer "Snapshot id: … · status … · ref …". Calls GET /snapshots/:id and the COA; emits notification:read.
  - Web error: Test timeout of 90000ms exceeded.

### Tax Filing — HMRC connection and VAT return

- **Submit gate and declaration (stubbed reads, no real write) @p0** — passed (account-hub/10-tax.spec.js:252)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Before calculating, read "Submit to HMRC". → After Calculate click it. → Click "Cancel".
  - Expected: Disabled until connected and calculated (title "Connect to HMRC first" when not connected); modal "Submit VAT return to HMRC" with the figures, "Legal declaration" text verbatim ("When you submit this VAT information you are making a legal declaration…") and "Agree and submit"; Cancel sends nothing.

### Cost Report — access & navigation

- **Accountant opens the worksheet from the sidebar @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:173)
  - Where: /film-tools/account-hub/cost-report · account: ACC
  - Steps: Log in as ACC. → Open Account Hub. → Click "Reports" › "Cost Report".
  - Expected: URL /film-tools/account-hub/cost-report; no Account Hub sidebar; header tabs "Cost Report Worksheet" and "Live CR"; back button "Back to Account Hub"; grid renders after the boot loader "Loading cost report…".

- **Non-accountant is redirected from every report route @p0 @smoke** — failed (account-hub/10-cost-report.spec.js:189)
  - Where: /film-tools/account-hub/cost-report · account: DEPT
  - Steps: Log in as DEPT. → Visit /film-tools/account-hub/cost-report, /trial-balance, /bible-report, /period-close in turn.
  - Expected: Each redirects to /film-tools. DEPT's Account Hub sidebar shows only Purchase Orders, Production Expense Cards, Petty Cash (no Reports or Management items).
  - Web error: Error: expect(locator).toBeVisible() failed

### Cost Report — viewing

- **Initial load sequence @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:217)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open the worksheet with network logging.
  - Expected: GET /api/v2/account-hub/chart-of-accounts?active_only=false and GET /api/v2/account-hub/budgets, then GET /api/v2/cost-reports/snapshots, then GET /api/v2/cost-reports/live (or GET /snapshots/:id for a frozen week). Sections in order ABOVE THE LINE, PRODUCTION, POST PRODUCTION, OTHER COSTS, CONTINGENCY (those present), then a "Grand Total — …" row.

### Cost Report — figures

- **Default forecast maths @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:254)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Pick a nominal with no override. → Read its figures.
  - Expected: ETC = max(0, Budget − ATD − all commits); EFC = ATD + commits + ETC; Total Variance = Budget − EFC, negatives shown as red "(£x)".

- **Headers and Grand Total roll up @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:274)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Expand one header. → Sum each column of its nominals.
  - Expected: Header row equals the sum of its nominals for every column; Grand Total equals the sum of headers; a .direct row (if any) is included.

### Cost Report — editing & versions

- **Edit ETC @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:389)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click a nominal's ETC cell. → Type 500, press Enter.
  - Expected: ETC 500; EFC = ATD + commits + 500; variance, header and Grand Total update; no network request.

### Cost Report — period lock, close and publish

- **Period Close defaults @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:540)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open "Period Close".
  - Expected: "Close Accounting Period"; "Last closed period" shows the lock date ("Locked through this date (inclusive).") or "No period locked yet"; "Close through date" min = lock + 1 and defaults to max(today, lock + 1); "Close Period" disabled for an earlier date; static guidance "If any transaction in this period is still unposted, the close will be rejected…".

### Trial Balance

- **Opens on Current Period with one request @p0 @smoke** — failed (account-hub/10-cost-report.spec.js:723)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open "Trial Balance".
  - Expected: Exactly one GET /api/v2/cost-reports/trial-balance after currencies load, with currency, period_start = lock date (or 2000-01-01), period_end = today, zero_accounts=false; rows grouped by account type with "Subtotal — <type>", "Trial Balance Total" and a "Balanced" pill when total debit = total credit.
  - Web error: Error: exactly one trial-balance request

### Bible Report

- **Runs only on the button @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:762)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open "Bible Report". → Click "Run Report".
  - Expected: No request on open ("Set filters and run the report"); Run → GET /api/v2/cost-reports/bible; accounts grouped with transactions (Src badge INV / CRED / CARD / CASH / PR, Eff Date, Invoice No., P/O No., Vendor / Employee, Description, Currency (Original), Amount), "Total <account>" and "Grand Total".

### Cost Report tool

- **Open the tool from the tile @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:852)
  - Where: /film-tools/cost-report · account: DEPT
  - Steps: Log in as DEPT. → Film Tools › "Cost Report".
  - Expected: URL becomes /film-tools/cost-report/current (replace); header with back button, tabs "Current CR" and "Posted CRs", "Analytics" button and the theme toggle; no Account Hub sidebar.

- **No tool right, no access @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:874)
  - Where: /film-tools/cost-report · account: NOTOOL
  - Steps: Log in as NOTOOL. → Check Film Tools. → Visit /film-tools/cost-report/current, /posted/<id>, /analytics.
  - Expected: No "Cost Report" tile; each URL redirects to /film-tools.

- **Load sequence @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:888)
  - Where: /film-tools/cost-report
  - Steps: Open Current CR with network logging.
  - Expected: Loader "Loading…" → "Fetching latest snapshot" → "Computing live report"; GET /api/v2/account-hub/chart-of-accounts?active_only=false + GET /budgets (once per session), GET /api/v2/cost-reports/snapshots, then GET /live for Mon 00:00 → Sun 23:59 browser time (or GET /snapshots/:id), plus GET /lock-period and GET /api/v2/account-hub/project-settings/companies.

- **Current CR matches the worksheet live figures @p0 @smoke** — failed (account-hub/10-cost-report.spec.js:924)
  - Where: /film-tools/cost-report
  - Steps: ACC: worksheet, current week, "All Companies — Consolidated", same budget/currency, Version "— Latest (unsaved) —". → Same filters on Current CR.
  - Expected: Budget, Actuals This Period, Actuals to Date and each Commits column agree per header and in the Grand Total.
  - Web error: Test timeout of 90000ms exceeded.

- **Posted CRs list @p0 @smoke** — passed (account-hub/10-cost-report.spec.js:955)
  - Where: /film-tools/cost-report
  - Steps: Click "Posted CRs". → Use "All" / "Daily" / "Wk-end". → "Refresh list".
  - Expected: GET /snapshots; "Posted Cost Reports", "{n} posts · last …"; rows newest first with chips Daily / WK / Lock / Initial, absolute total_variance (negative red in parentheses) and a "vs last" delta; Wk-end shows WK, Initial and Lock; Refresh re-calls the list; empty filter "No posts in this filter."

### Tax Filing — access & navigation

- **Accountant opens the hub (dev) @p0 @smoke** — passed (account-hub/10-tax.spec.js:55)
  - Where: /film-tools/account-hub/tax-filing · account: ACC
  - Steps: As ACC on dev click "Management" › "Tax Filing".
  - Expected: URL /film-tools/account-hub/tax-filing; sticky header with back chip and breadcrumb "Management / Tax Filing"; title "Tax Filing" with subtitle "Choose a country and tax regime to file…"; tab title "Tax Filing · Zillit"; "Skip to main content" link is the first focusable element.

- **Staging/production show Coming Soon @p0 @smoke** — skipped (account-hub/10-tax.spec.js:65)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: On staging open the hub. → Visit /tax-filing/GB/mtd-vat?connected=1.
  - Expected: Header + "Coming Soon" placeholder "Filing submissions will be available here soon."; no /api/v2/tax-filing request; the filing URL is replaced with the hub (query dropped, no toast).

### Tax Filing — registrations

- **Register modal validation @p0 @smoke** — passed (account-hub/10-tax.spec.js:98)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: "Register a company". → Type 12a34 in the VRN field, then 8 digits, then 9. 3. Leave Company empty.
  - Expected: Modal "Register a VAT number"; letters stripped, max 9 digits; with 8 digits "A VRN is 9 digits — you’ve entered 8. Enter all 9 digits, numbers only."; "Register VRN" disabled until a company and 9 digits; Company list hides registered companies ("Every available company is already registered." when none left); Filing frequency Monthly / Quarterly (default) / Annually.

- **Register a VRN @p0** — passed (account-hub/10-tax.spec.js:141)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Pick the UK company, sandbox VRN, a registration date, Quarterly. → "Register VRN".
  - Expected: POST /api/v2/tax-filing/registrations {company_id, country_code:"GB", regime:"VAT", registration_number, registration_date, filing_frequency:"quarterly"}; success toast and dark pill "Registered <company>"; card "VRN 123 456 789 · Quarterly filing", pill "active", "Connect to HMRC" + "required to file a return", "Open VAT return" disabled with title "Connect to HMRC to file a VAT return".

### Tax Filing — HMRC connection and VAT return

- **Connect to HMRC @p0** — skipped (account-hub/10-tax.spec.js:181)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: "Connect to HMRC" on an unconnected card. → Sign in to the HMRC sandbox and grant access.
  - Expected: Button "Connecting…"; GET /api/v2/tax-filing/GB/oauth/authorize?registrationId=<id>; full-page redirect to HMRC; return to …/GB/mtd-vat?connected=1, toast "Connected to HMRC", query stripped, list reloads, card shows "Connected to HMRC".

- **Sync obligations @p0** — skipped (account-hub/10-tax.spec.js:187)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: "Sync obligations".
  - Expected: Button "Syncing…"; POST …/GB/registrations/<id>/obligations/sync {from, to, fraudData} (one year back to today); toast "Synced obligations from HMRC"; dropdown lists periods as dates (DD/MM/YYYY), never the period key; empty → "No obligations — sync to fetch from HMRC."

- **Calculate from the ledger @p0** — skipped (account-hub/10-tax.spec.js:209)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: "Calculate from ledger". → Compare rail values with the ledger.
  - Expected: PUT /box-map then POST …/returns/draft {periodKey}; rail shows every box, Box 3 = Box 1 + Box 2, Box 5 = Box 3 − Box 4 with "To pay" / "To reclaim" and "Payable to HMRC" / "Reclaimable from HMRC"; Boxes 6–9 whole pounds; "Calculated just now from the ledger"; button becomes "Recalculate from ledger"; Box 4 equals the summed debit − credit of its mapped codes for the quarter.

- **Submit the return (sandbox) @p0** — skipped (account-hub/10-tax.spec.js:296)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: "Agree and submit".
  - Expected: POST …/GB/registrations/<id>/returns/submit {periodKey, payload:{…draft.payload, finalised:true}, fraudData}; toast "Return submitted to HMRC"; draft cleared; obligations re-synced and returns reloaded; period turns "Fulfilled" and the Filed return panel shows the receipt.

- **Box-map load failure wipes the mapping (stubbed) @p1** — bug (account-hub/10-tax.spec.js:317)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Company with a saved mapping. → Route GET /box-map → 500. → Click "Save mapping" (or Calculate).
  - Expected: Expected: save blocked. Code sends PUT /box-map with rows: [], erasing the company's mapping for every period (K).
  - Web error: Error: no PUT /box-map after a failed load

### Access & navigation › as CREW-HUB (crew + account_hub_tool view, stubbed)

- **Non-accountant is redirected off Approvers @p0 @smoke** — passed (account-hub/02-setup-approvers.spec.js:93)
  - Where: /film-tools/account-hub/approvers
  - Steps: As B open /film-tools/account-hub/approvers (also in an MDI window).
  - Expected: Lands on Purchase Orders.

### Access & navigation

- **Accountant opens Approvers @p0 @smoke** — passed (account-hub/02-setup-approvers.spec.js:72)
  - Where: /film-tools/account-hub/approvers
  - Steps: As A click CONFIGURATION › "Approvers".
  - Expected: Module list under "Modules": TRANSACTIONS (Purchase Orders, Invoices / Accounts Payable, Production Expense Cards, Petty Cash Expenses) and PAYROLL MANAGEMENT (Time Card, Deal Memo); Purchase Orders selected; GET /account-hub/approval-tiers/summary and GET /account-hub/approval-tiers?module=purchase_orders.

### Access & navigation › as CREW-HUB (crew + account_hub_tool view, stubbed)

- **Non-accountant is redirected off Chart of Accounts and Budget @p0 @smoke** — passed (account-hub/02-setup-coa.spec.js:90)
  - Where: 02 Setup and configuration
  - Steps: As B open /film-tools/account-hub/chart-of-accounts, then /budget, also in an MDI window.
  - Expected: Each lands on Purchase Orders.

### Access & navigation

- **Accountant opens Chart of Accounts @p0 @smoke** — passed (account-hub/02-setup-coa.spec.js:72)
  - Where: 02 Setup and configuration
  - Steps: As A click CONFIGURATION › "Chart of Accounts".
  - Expected: Title "Chart of Accounts"; tabs "Cost Accounts" (selected), "Balance Sheet Codes", "Layers"; one GET /account-hub/chart-of-accounts?active_only=false.

### Forms Configuration › as CREW-HUB (crew + account_hub_tool view, stubbed)

- **Non-accountant is redirected off Forms Configuration @p0 @smoke** — passed (account-hub/02-setup-forms-vendors.spec.js:101)
  - Where: /film-tools/account-hub/form-config
  - Steps: As D open /film-tools/account-hub/form-config (also in an MDI window).
  - Expected: Lands on Purchase Orders.

### Forms Configuration

- **Accountant opens Forms Configuration @p0 @smoke** — failed (account-hub/02-setup-forms-vendors.spec.js:81)
  - Where: /film-tools/account-hub/form-config
  - Steps: As A click CONFIGURATION › "Forms Configuration".
  - Expected: Module rail with "Purchase Orders" and "Petty Cash Expenses"; GET /account-hub/form-templates?module=purchase_orders; card "Default Form Configuration" with sections and fields, "Edit Fields", "Reset to Defaults", "Set Approver Level".
  - Web error: Error: Edit Fields

### Vendors › as CREW-HUB (crew + account_hub_tool view, stubbed)

- **Non-accountant is redirected off the vendor register @p0 @smoke** — passed (account-hub/02-setup-forms-vendors.spec.js:280)
  - Where: /film-tools/account-hub/vendors
  - Steps: As D open /film-tools/account-hub/vendors/all (also in an MDI window).
  - Expected: Lands on Purchase Orders.

### Vendors

- **Accountant opens the vendor register @p0 @smoke** — passed (account-hub/02-setup-forms-vendors.spec.js:264)
  - Where: /film-tools/account-hub/vendors
  - Steps: As A click MANAGEMENT › "Vendors".
  - Expected: URL …/vendors/all; title "Vendors" with "Manage supplier records, verify vendor details, and maintain your approved vendor list."; tabs "All Vendors", "Verified", "Non-Verified" with counts; GET /api/v2/vendors.

### Access & navigation › as CREW-HUB (crew + account_hub_tool view, stubbed)

- **Non-accountant is redirected off Production Setup @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:91)
  - Where: /film-tools/account-hub/production-setup · account: non-accountant B
  - Steps: Log in as non-accountant B. → Open /film-tools/account-hub/production-setup directly.
  - Expected: Redirected to /film-tools/account-hub/purchase-orders; no Production Setup content renders.

### Access & navigation

- **Accountant opens Production Setup from the sidebar @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:74)
  - Where: /film-tools/account-hub/production-setup · account: accountant A
  - Steps: Log in as accountant A. → Click SETUP › "Production Setup".
  - Expected: URL is /film-tools/account-hub/production-setup. Header eyebrow "Setup", title "Production Setup", description "Project-wide accounting defaults inherited by purchase orders, invoices, expenses, payroll runs and time cards."

- **Admin Settings opens the Companies-only view @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:103)
  - Where: /film-tools/account-hub/production-setup
  - Steps: As A, open Admin Settings. → Click "Production Setup".
  - Expected: URL /settings/production-setup?s=admin. Header "Production Setup" with description "The production entities on this project. Each company owns its bank accounts…" and a back button. Only the Companies card shows: no Bank Accounts card, no currencies, tags, tax or tiles. Back returns to /settings?s=admin.

### Company and bank modals (no writes)

- **New company modal fields @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:133)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Click "Add company".
  - Expected: Modal "New company" with "Company / Entity name *" (placeholder "e.g. Acme Productions Ltd"), the tick "Tick if Company's legal name is different from above", "Country *", "Tax credit tagging", "Bank accounts", and buttons "Cancel" and "Add company".

- **Add company stays disabled until name and country @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:153)
  - Where: /film-tools/account-hub/production-setup
  - Steps: "Add company". → Type a name only. → Pick a country only (clear name).
  - Expected: "Add company" is disabled until both name and country are set.

- **Add Bank Account modal fields @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:172)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Click "Add account".
  - Expected: Modal "Add Bank Account": "Bank Name *", "Account Holder *" with "+ Add company", "Sort Code", "Account Number *", "IBAN", "SWIFT / BIC", "Cheque Number", "Wire Number", "Bank Account Nominal Code *", "AP Clearance Nominal Code *", "Currency *", "Additional Details"; buttons "Cancel" and "Save Account".

- **Bank required fields fail in order @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:194)
  - Where: /film-tools/account-hub/production-setup
  - Steps: "Save Account" on an empty form. → Fill each field in turn and press save again.
  - Expected: Inline red text in order: "Bank name is required." → "Account holder company is required." → "Account number is required." → "Bank account nominal code is required." → "AP clearance nominal code is required." → "Currency is required."; no request until all pass.

- **"+ Add company" after a failed settings refetch keeps every company @p0** — failed (account-hub/02-setup-production.spec.js:222)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Load the page, then stub GET /account-hub/project-settings to fail and trigger a refetch (save any section). → Open "Add account" › "+ Add company", create one.
  - Expected: Expected: refused until companies load. Code: PATCH body is [newCompany] only, which deletes every other company. Run only against stubs.
  - Web error: Error: existing company new test kept

### Project Currencies

- **First pick becomes the default @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:557)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Click the GBP tile.
  - Expected: GBP card shows "Default" and "BASE · 1"; "Default currency" picker appears; card header shows "Unsaved" and "Save changes".

- **A missing exchange rate blocks the save @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:577)
  - Where: /film-tools/account-hub/production-setup
  - Steps: With GBP default, add EUR. → Leave "Rate" empty, "Save changes".
  - Expected: Error toast "Add an exchange rate for EUR before saving."; EUR card flagged; no PATCH.

### Account Tags and Tax Types

- **Add a tax country (every catalogue tax checked) @p0 @smoke** — passed (account-hub/02-setup-production.spec.js:698)
  - Where: /film-tools/account-hub/production-setup
  - Steps: In "Add another country…" pick United Kingdom.
  - Expected: A GB group with every catalogue tax checked; left panel shows "{n} active tax rates across 1 country".

### Companies and bank accounts

- **Create a company @p0** — passed (account-hub/02-setup-production.spec.js:297)
  - Where: /film-tools/account-hub/production-setup
  - Steps: "Add company". → Name E2E-<run>-Co, Country "United Kingdom". → "Add company".
  - Expected: PATCH /account-hub/project-settings/companies; server success toast; modal closes; new card shows "Production Co.", the name, a flag chip with the country, "Active · 0 accounts" and Linked banks "None".

- **Companies PATCH sends the whole list @p0** — passed (account-hub/02-setup-production.spec.js:325)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Note the companies before. → Create one company. → Inspect the request body.
  - Expected: Body is an array of every existing company plus the new one; existing ids unchanged; each row carries id, name, legal_name, country, country_code, uk{4 keys}, bank_ids, tax_credits; names are trimmed.

- **Create a production bank @p0** — passed (account-hub/02-setup-production.spec.js:368)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Fill name, pick holder company, account number, both nominal codes, currency. → "Save Account".
  - Expected: POST /account-hub/bank-accounts with entity_type: "production", entity_id = company id, account_holder_name = company name, currency: {code,name,symbol}; success toast; modal closes; a card appears without a full-list loader.

- **Unticking a bank releases it @p0** — failed (account-hub/02-setup-production.spec.js:390)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Edit a company that owns bank → 2. Untick it, "Done".
  - Expected: After the companies PATCH, PATCH /account-hub/bank-accounts/{id} with { entity_id: null, account_holder_name: "" }; bank card "Holder" shows "—"; the bank is offered to other companies.
  - Web error: Error: expect(locator).toHaveCount(expected) failed

- **Bank picker hides banks owned by another company @p0 @smoke** — notrun (account-hub/02-setup-production.spec.js:420)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Company X owns bank 1; bank 2 is unclaimed. → Edit company Y.
  - Expected: "Bank accounts" lists bank 2 (and Y's own banks), not bank 1.

- **Link a bank to a company @p0** — notrun (account-hub/02-setup-production.spec.js:441)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Edit company Y. → Tick bank 2, "Done".
  - Expected: Body has bank 2 in Y's bank_ids and removes it from any other company; card shows "Active · 1 account", a "Linked banks" chip and a currency pill with the bank's currency code.

- **Delete a bank (after confirming) @p0** — notrun (account-hub/02-setup-production.spec.js:466)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Click the trash ("Remove account"). → Confirm "Delete".
  - Expected: Nothing is deleted before confirming. Confirm "Remove bank account" says "Remove "{name}" ending {last4}? This can't be undone."; "Delete" sends DELETE /account-hub/bank-accounts/{id}; the card disappears with no loader flash.

- **Remove a company (card trash and edit modal) @p0** — notrun (account-hub/02-setup-production.spec.js:494)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Click the trash (aria-label "Remove company"). → Confirm "Delete". → Repeat from the edit modal's "Remove company".
  - Expected: Confirm "Remove company" says "Remove "{name}"? This can't be undone."; "Delete" shows "Deleting…" and sends the list without that company; the card disappears; from the edit modal, the modal also closes. "Cancel" sends nothing.

### Project Currencies

- **Save currencies and rates @p0** — passed (account-hub/02-setup-production.spec.js:603)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Enter EUR rate "1.17". → "Save changes".
  - Expected: PATCH /account-hub/project-settings/project-currencies body { currencies: [{code:"GBP",…,exr:1},{code:"EUR",…,exr:1.17}], default: "GBP" }; success toast; "Unsaved" disappears.

### Account Tags and Tax Types

- **Save account tags @p0** — passed (account-hub/02-setup-production.spec.js:648)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Add tags. → "Save changes".
  - Expected: PATCH /account-hub/project-settings/asset-tags with a de-duplicated uppercase string array; success toast. The new tags are offered in a PO line "Tags" picker afterwards (section 03).

- **Save tax types @p0** — failed (account-hub/02-setup-production.spec.js:721)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Save.
  - Expected: PATCH /account-hub/project-settings/tax-types rows { type, identifier: "GB_<id>", label, value (number), country, country_code: "GB", is_recoverable, nominal }; success toast.
  - Web error: Error: expect(received).toEqual(expected) // deep equality

### Entry Setup tiles

- **Save the Purchase Order entry setup @p0** — passed (account-hub/02-setup-production.spec.js:770)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Pick "ITEM → DDMON". → "Default split type" = "Monthly". → "PO number prefix" type "ab-12!xyz9". → Save, close, re-open.
  - Expected: Prefix shows "AB12XYZ9" (uppercase, alphanumeric, 8 max); PATCH /purchase-orders/settings; success toast; re-open shows the stored values.

### Vouchers, signing and queries (Lava as an internal artiste)

- **"Action needed" lists every submitted voucher, under All only @p0** — passed (account-hub/11-supporting-artists.spec.js:211)
  - Where: /film-tools/supporting-artists
  - Steps: Have two submitted vouchers. → Switch filter to "To sign".
  - Expected: Under "All" an "Action needed" block lists every day_status: submitted voucher with "Review & Sign"; the block is hidden under other filters.

- **Dashboard action banner opens the voucher @p0** — passed (account-hub/11-supporting-artists.spec.js:232)
  - Where: /film-tools/supporting-artists
  - Steps: With a submitted voucher, open Dashboard. → Click "Review & sign".
  - Expected: Banner "Action needed — a voucher is awaiting your signature" with date, gross and "payroll cannot process until signed" (from summary.action_voucher); button opens /voucher/<code>. No banner without action_voucher.

- **Artistes see only their own vouchers (2U) @p0** — passed (account-hub/11-supporting-artists.spec.js:249)
  - Where: /film-tools/supporting-artists
  - Steps: Artiste A and artiste B each open My Vouchers and Queries.
  - Expected: Each sees only their own vouchers and queries (server-scoped); opening B's voucher code as A shows "Voucher not found".

- **Voucher detail content @p0** — failed (account-hub/11-supporting-artists.spec.js:269)
  - Where: /film-tools/supporting-artists
  - Steps: Open a voucher.
  - Expected: URL /voucher/<code>; crumb "Vouchers" with Back; GET /api/v2/sa-portal/vouchers/:id; Call time / Wrap time shown exactly as stored (UTC wall-clock, e.g. 07:30 for every viewer), duration, "Pay breakdown" (rates_ots + allowances; no additional fees, no meals), "Gross Total", "Holiday pay (12.07% accrued)", "Rates & Overtime", "Production Info", "Actions". No lines → "Itemized breakdown not available — gross total below."
  - Web error: Error: expect(locator).toBeVisible() failed

- **Sign controls gate the submit button @p0** — notrun (account-hub/11-supporting-artists.spec.js:285)
  - Where: /film-tools/supporting-artists
  - Steps: On a submitted voucher click "Add signature" and pick a saved signature. → Tick one consent, then both.
  - Expected: Preview shows; "Change signature" appears; "Sign & Submit Voucher" stays disabled until a signature and both consents ("I confirm the times, pay and allowances on this voucher are accurate…", "I agree to use an electronic signature…") are set.

- **Sign and submit a voucher @p0** — notrun (account-hub/11-supporting-artists.spec.js:303)
  - Where: /film-tools/supporting-artists
  - Steps: Click "Sign & Submit Voucher". → Reopen the voucher.
  - Expected: "Signing…"; signature PNG uploaded to project storage (caption "Voucher signature"); POST /api/v2/sa-portal/vouchers/:id/sign {consent_accuracy:true, consent_esign:true, sign_status:'e-sign', sign_attachment, typed_name}; toast "Voucher signed & submitted — payroll will process this week"; back on My Vouchers with pill "Signed". Detail shows "Signed by {name}", method "Drawn signature", signed time (viewer's zone), "Awaiting payroll", and both consent lines.

- **Sign card only on a submitted, unsigned voucher @p0** — notrun (account-hub/11-supporting-artists.spec.js:332)
  - Where: /film-tools/supporting-artists
  - Steps: Open a Pending, a submitted, and a signed voucher.
  - Expected: Only the submitted, unsigned one shows "Sign this Voucher" (with the Electronic Communications Act / eIDAS note); the signed one shows the signed proof instead; Pending shows neither.

- **The AD's list shows the signature without reload (2U) @p0** — notrun (account-hub/11-supporting-artists.spec.js:347)
  - Where: /film-tools/supporting-artists
  - Steps: AD has Today's List (or the day) open. → Artiste signs.
  - Expected: AD's row turns "✓ Signed" without reload (sa:voucher:updated → list refetch).

- **Query composer fields and validation @p0** — notrun (account-hub/11-supporting-artists.spec.js:372)
  - Where: /film-tools/supporting-artists
  - Steps: Click "New".
  - Expected: Overlay "Raise a new query", "Goes straight to the AD & production accountant."; "Which day is this about?" (every voucher as "<date> · <code>", placeholder "Select a shoot day…"); "Topic" default "Wrap / call time" (also Overtime calculation, Missing allowance, Holiday pay, Payment not received, Something else); "Priority" Low / Normal (default) / Urgent; "Describe the issue" capped at 500 characters. "Submit query" disabled until a day and non-blank text.

- **Submit a new query; the AD sees it (2U) @p0** — notrun (account-hub/11-supporting-artists.spec.js:402)
  - Where: /film-tools/supporting-artists
  - Steps: Pick a day with no thread, type text, "Submit query".
  - Expected: POST /api/v2/sa-portal/queries {topic, text, voucher_id, priority}; toast "Query raised — the production team has been notified"; composer closes; the query appears "Active". AD's Artiste Queries shows it with a badge.

- **Profile edit validation @p0** — notrun (account-hub/11-supporting-artists.spec.js:427)
  - Where: /film-tools/supporting-artists
  - Steps: "Edit details". → Clear DOB, e-mail, mobile, address line 1, city, state/county, postcode, nationality, tax document type and number, bank name/holder/number/IBAN, ID document type, Category; add a document row with a label and no file. → Save.
  - Expected: No crew picker or first/last-name fields; toast "Please complete all required fields."; inline "Required.", "Email is required.", "Tax reference is required.", "Enter an account number or an IBAN", "Attachment required."; "Other" types need "Specify document type". Union opens as "Non-union" and never blocks; e-mail has no asterisk but is required.

- **Save the profile; the AD register refreshes (2U) @p0** — notrun (account-hub/11-supporting-artists.spec.js:444)
  - Where: /film-tools/supporting-artists
  - Steps: Change the mobile number and attach a document; save.
  - Expected: New files upload to project storage first; PATCH /api/v2/sa-portal/me with the full nested artiste and bank (no bank_id); toast "Your details have been updated"; back to the read view with new data. The AD's Artiste Register/Artiste page refreshes (sa:artiste:updated).

- **A day the AD submits appears live on My Vouchers (2U) @p0** — notrun (account-hub/11-supporting-artists.spec.js:469)
  - Where: /film-tools/supporting-artists
  - Steps: Artiste sits on My Vouchers. → AD submits the day.
  - Expected: Within about a second the row turns "Sign now" and "Action needed" lists it, with no skeleton flash (ad_dashboard:ad_shoot_day:submitted / sa:voucher:updated → silent reload).

### Routing, entry modes and access › Route families and redirects › as CREW-HUB (crew + account_hub_tool view, stubbed)

- **Hub root lands a department user on the bare PO path @p0 @smoke** — passed (account-hub/01-shell.spec.js:99)
  - Where: /film-tools/account-hub/* · account: CREW-HUB
  - Steps: Log in as CREW-HUB. → Open /film-tools/account-hub.
  - Expected: URL /film-tools/account-hub/purchase-orders (no tab segment); "Approval Queue" tab active; no hub sidebar.

### Routing, entry modes and access › Route families and redirects

- **Hub root lands an accountant on the PO Queue @p0 @smoke** — passed (account-hub/01-shell.spec.js:86)
  - Where: /film-tools/account-hub/* · account: ACC-PA
  - Steps: Log in as ACC-PA. → Open /film-tools/account-hub.
  - Expected: URL replaced with /film-tools/account-hub/purchase-orders/queue/my; "Queue" tab active; sidebar header reads "Account Hub".

### Routing, entry modes and access › Film Tools tiles and entry mode

- **Account Hub tile follows account_hub_tool view access @p0 @smoke** — passed (account-hub/01-shell.spec.js:115)
  - Where: /film-tools/account-hub/* · account: ACC-PA
  - Steps: As ACC-PA open /film-tools. → As CREW-TOOL open /film-tools.
  - Expected: 1: Account Hub tile shown; click → PO Queue with sidebar. 2: no Account Hub tile; Purchase Order, Card and Cash tiles shown.

- **Purchase Order tile gives an accountant the crew view @p0 @smoke** — passed (account-hub/01-shell.spec.js:141)
  - Where: /film-tools/account-hub/*
  - Steps: As ACC-PA click the Purchase Order tile. → Click the header back button (aria-label "Back to film tools").
  - Expected: 1: URL …/purchase-orders?entry=tool; department tabs ("Approval Queue", "My POs"…; "All POs" only if ACC-PA is admin or has PO posting), not the accountant "Queue" / "PO Entry" tabs; no hub sidebar; header shows back button and round theme toggle. 2: /film-tools.

- **Tool mode survives in-module tab switches @p0 @smoke** — passed (account-hub/01-shell.spec.js:160)
  - Where: /film-tools/account-hub/*
  - Steps: Continue from AH-SH-012 step 1. 2. Click "My POs". 3. Click "Approval Queue".
  - Expected: URL drops ?entry=tool, but the hub sidebar never appears and the department view stays.

### Routing, entry modes and access › Roles and accountant-only routes › as CREW-HUB (crew + account_hub_tool view, stubbed)

- **Department user is redirected off accountant-only hub routes @p0 @smoke** — passed (account-hub/01-shell.spec.js:192)
  - Where: /film-tools/account-hub/* · account: CREW-HUB
  - Steps: As CREW-HUB open /film-tools/account-hub/vendors. → Repeat for approvers, form-config, guide, production-setup, chart-of-accounts, budget.
  - Expected: Each replaces to /film-tools/account-hub/purchase-orders (department landing); the accountant screen never renders.

- **Department user is redirected off accountant-only full-bleed routes @p0 @smoke** — passed (account-hub/01-shell.spec.js:206)
  - Where: /film-tools/account-hub/* · account: CREW-HUB
  - Steps: As CREW-HUB open /film-tools/account-hub/cost-report. → Repeat for trial-balance, bible-report, period-close, tax-filing, payroll/config.
  - Expected: Each replaces to /film-tools.

### Routing, entry modes and access › Roles and accountant-only routes

- **Any accounts-department member is an accountant @p0** — skipped (account-hub/01-shell.spec.js:185)
  - Where: /film-tools/account-hub/* · account: ACC-JR
  - Steps: Log in as ACC-JR. → Open /film-tools/account-hub.
  - Expected: Hub sidebar shown and landing on "Queue", same as ACC-PA.

### Routing, entry modes and access › Payroll gate

- **Producer with payroll view uses the Payroll tile @p0 @smoke** — passed (account-hub/01-shell.spec.js:222)
  - Where: /film-tools/account-hub/* · account: PROD
  - Steps: Log in as PROD. → Click the Payroll tile. → Open one producer tile.
  - Expected: Landing shows producer tiles (no "Payroll Entry Setup"); the tile route /film-tools/account-hub/payroll/<tile> opens; never bounced, although PROD has no account_hub_tool.

- **Non-accountant without payroll view is sent to Film Tools @p0 @smoke** — passed (account-hub/01-shell.spec.js:242)
  - Where: /film-tools/account-hub/* · account: CREW-HUB
  - Steps: Log in as CREW-HUB. → Open /film-tools/account-hub/payroll?entry=tool. → Open /film-tools/account-hub/payroll/processing.
  - Expected: Both replace to /film-tools once GET /v2/project/tools has loaded.

### Routing, entry modes and access › View gate and live revocation › as the crew account

- **Tool-tile entry needs only that tool @p0 @smoke** — passed (account-hub/01-shell.spec.js:273)
  - Where: /film-tools/account-hub/*
  - Steps: As CREW-TOOL click the Purchase Order tile. → Type /film-tools/account-hub/purchase-orders (no marker).
  - Expected: 1 opens and stays. 2 replaces to /film-tools (hub entry needs account_hub_tool).

- **Standalone tools need their own tool @p0 @smoke** — passed (account-hub/01-shell.spec.js:286)
  - Where: /film-tools/account-hub/*
  - Steps: As a user lacking timecard_tool view open /film-tools/timecard. → Repeat for deal-memo (deal_memo_tool), talent-deals (talent_deals_tool), cost-report incl. /analytics and /posted/<id> (cost_report_tool), ad-dashboard (ad_dashboard_tool), supporting-artists (supporting_artistes_extras_tool), asset-report (asset_report_tool).
  - Expected: Each replaces to /film-tools; holding account_hub_tool does not help.

### Routing, entry modes and access › View gate and live revocation › live revocation (changes the crew account's rights, restored)

- **Live revocation of hub access (classic) @p0** — passed (account-hub/01-shell.spec.js:322)
  - Where: /film-tools/account-hub/*
  - Steps: ACC-JR opens sidebar "Vendors". → In another context ADMIN revokes ACC-JR's Account Hub viewing right. → Restore afterwards.
  - Expected: Within seconds of the rights event ACC-JR is replaced to /film-tools without a reload; the Account Hub tile is gone.

- **Revoking a tile tool ejects tile users only @p0** — passed (account-hub/01-shell.spec.js:358)
  - Where: /film-tools/account-hub/*
  - Steps: CREW-HUB opens the Card Expenses tile; ACC-JR opens "Production Expense Cards" from the hub. → ADMIN revokes card_expenses_tool viewing for both. → Restore.
  - Expected: CREW-HUB is ejected to /film-tools; ACC-JR (hub entry, still holds account_hub_tool) stays.

### Routing, entry modes and access › View gate and live revocation

- **Hub entry needs only account_hub_tool @p0 @smoke** — passed (account-hub/01-shell.spec.js:255)
  - Where: /film-tools/account-hub/* · account: ADMIN remove Purchase Order
  - Steps: As ADMIN remove Purchase Order in Tool Customization (keep Account Hub). → As ACC-PA click the Account Hub tile. → Restore the tool.
  - Expected: Hub opens on the PO Queue and stays (no bounce through the catch-all to /film-tools).

### Hub chrome: sidebar, windows, theme and scroll

- **Sidebar header and back button @p0 @smoke** — passed (account-hub/01-shell.spec.js:409)
  - Where: /film-tools/account-hub/* · account: ACC-PA
  - Steps: As ACC-PA open the hub. → Click the back button (aria-label "Back to film tools").
  - Expected: Header card reads "Account Hub"; click → /film-tools.

### Public portal OTP gate

- **First entry shows step 1 and keeps the page closed @p0 @smoke** — passed (account-hub/01-shell.spec.js:524)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Logged out, open /deal-memo/crew-portal/<token>.
  - Expected: Header: Zillit logo, "Deal Memo Portal", badge "Crew Portal"; card "STEP 1 OF 2", "Verify Your Identity", "This deal memo is protected. Enter the email address associated with the deal to receive a one-time code."; field "Email Address"; footnote "The code is sent only to the email on the deal record. It is never shown on screen."; the gated GET …/deal-memo/crew-portal/<token> is not called.

- **Requesting a code moves to step 2 (stubbed request-otp) @p0 @smoke** — passed (account-hub/01-shell.spec.js:546)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Enter the deal's email; click "Send Verification Code".
  - Expected: "STEP 2 OF 2", "Enter Verification Code", "We sent a 6-digit code to j****@… It expires in 15 minutes and can be used once." using the server's masked address; six boxes, first focused; "Code expires in 14:5x"; "Resend in 30s".

- **Requesting a code moves to step 2 (real email) @p0** — skipped (account-hub/01-shell.spec.js:577)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Enter the deal's email; click "Send Verification Code".
  - Expected: "STEP 2 OF 2", "Enter Verification Code", "We sent a 6-digit code to j****@… It expires in 15 minutes and can be used once." using the server's masked address; six boxes, first focused; "Code expires in 14:5x"; "Resend in 30s".

- **Verifying opens the portal with a session strip (stubbed verify) @p0 @smoke** — passed (account-hub/01-shell.spec.js:582)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Enter the emailed code; click "Verify & Continue".
  - Expected: Button reads "Verifying…", then the deal loads (GET …/deal-memo/crew-portal/<token>); a "Verified." notice "You're viewing this deal memo as a verified recipient." with "Session expires in 59:5x" counting down.

- **Verifying opens the portal with a session strip (real code) @p0** — skipped (account-hub/01-shell.spec.js:639)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Enter the emailed code; click "Verify & Continue".
  - Expected: Button reads "Verifying…", then the deal loads (GET …/deal-memo/crew-portal/<token>); a "Verified." notice "You're viewing this deal memo as a verified recipient." with "Session expires in 59:5x" counting down.

### Realtime refresh and badges

- **Approval queue: opening does not clear, acting does @p0** — skipped (account-hub/01-shell.spec.js:422)
  - Where: 01 Shell, access and cross-cutting
  - Steps: CREW-HUB (approver) has an unread PO in "Approval Queue". → Open it and close it. → Open it again and approve.
  - Expected: After 2 the badge is unchanged; after a successful approve it drops by one. The same rule holds in the cash, invoice, card, deal memo, time card and payment-run queues.

### Weekly time card › first open and reopen

- **First open creates the draft @p0** — passed (account-hub/08-timecard-weekly.spec.js:122)
  - Where: /film-tools/timecard · account: Crew A
  - Steps: As Crew A open the "Weekly Time Card" tile for a week with no card.
  - Expected: Order: metadata → GET /deal-memo/deal?status=active → OT manifest/bundle → GET /weekly?week_starting=<UTC midnight of pay-period start> → POST /weekly with week_starting, department_id, phase, 7 skeleton days; pill "DRAFT"; header "Week N · DD Mon–DD Mon YYYY (Phase)".

- **Reopen uses the same card @p0** — passed (account-hub/08-timecard-weekly.spec.js:173)
  - Where: /film-tools/timecard
  - Steps: Leave and reopen Weekly for the same week.
  - Expected: One GET /weekly, no POST /weekly; same _id in later PATCHes.

### Weekly time card

- **Non-fillable days are greyed @p0** — passed (account-hub/08-timecard-weekly.spec.js:203)
  - Where: /film-tools/timecard
  - Steps: Open the week containing deal_start (and the current week).
  - Expected: Days before deal_start, after last_pay_date and after today are disabled: no inputs, type cell shows "Upcoming" when untyped.

### Day types

- **Switching type: shoot keeps data, flat-pay clears it @p0** — passed (account-hub/08-timecard-weekly.spec.js:250)
  - Where: /film-tools/timecard
  - Steps: Fill SWD times, a meal and an allowance; switch to CWD. → Switch to "Prep".
  - Expected: CWD keeps times, meals and allowance. Prep clears Unit Call / Unit Wrap, My Call / My Wrap, meals, NDM, allowances and extras; time cells show "Auto · daily rate"; Day Total = basic line only; saved day has no times and one "Basic Day Rate" line in rates_ots.

- **Non-paid types collapse the row @p0** — passed (account-hub/08-timecard-weekly.spec.js:297)
  - Where: /film-tools/timecard
  - Steps: Pick "Day Off" on a filled day; save and reload. → Set other days to "Holiday" and "Sick (Unpaid)"; open a legacy card with Sick.
  - Expected: Merged row with no inputs, Day Total 0; saved day day_type:"REST" with empty rates_ots, allowances, meals. Merged labels read "Day Off", "Holiday", "Sick (Unpaid)" (printed as-is) and "Sick Day" for legacy Sick.

### Assigned time cards & Review Inbox (two users)

- **Owner assigns and unassigns a completer @p0** — passed (account-hub/08-timecard-assigned.spec.js:100)
  - Where: /film-tools/timecard
  - Steps: On a draft week click "Assign". → Click the picker, search, pick Completer B. → Choose "This week", click "Save Assignment". → Click "Unassign" on B.
  - Expected: Modal "Assignment Settings" ("The assigned person cannot see your pay rates…"); picker "Select Crew Member" with "Department (n)" / "Other Users (n)", own user excluded; "Save Assignment" disabled until a user is picked; POST /assignments {assigned_to, assignment_date_range:{start_date,end_date}} ("All weeks" sends null); B listed under "Time Card Assignment". "Unassign" sends DELETE /assignments/:id with no confirm; "No completer assigned for this week." when none is left.

- **Completer's queue @p0** — passed (account-hub/08-timecard-assigned.spec.js:160)
  - Where: /film-tools/timecard
  - Steps: As B open "Assigned Time Card". → Use "All" / "Pending" / "In Review".
  - Expected: GET /assignments/all; one row per owner × week with "This week" tag and status chip; Pending = rows with pending or returned days, In Review = rows with under-review days; empty → "No assignments found" / "You have no timecards assigned to you." ("No assignments match this filter." when filtered).

- **Open a crew week @p0** — passed (account-hub/08-timecard-assigned.spec.js:182)
  - Where: /film-tools/timecard
  - Steps: Click Crew A's row.
  - Expected: GET /deal-memo/deals/schedule/<userId>; chip "Rates hidden"; banner "Completing on behalf of <name>." / "Pay rates are not visible. Times sourced automatically where available — check and fill any gaps." / "<first name> will review and approve before going to the producer."; badge read for the row. For Crew E (no active deal) the banner reads "<name> has no active deal memo. Times can still be entered, but deal-based pay rates, schedule, and entitlements won’t apply until their deal memo is activated."

- **Inline times save via batch; Save Draft @p0** — failed (account-hub/08-timecard-assigned.spec.js:198)
  - Where: /film-tools/timecard
  - Steps: Set Time In 08:00, Time Out 19:00 and a meal on a pending day. → Add an allowance. → Click "Save Draft", then click it again with no edits.
  - Expected: Auto-save PATCH /daily-login/batch with user_id = Crew A; when allowances changed, a PATCH /weekly/:id (live days overlaid) goes first. "Save Draft" shows the server toast, then "No changes to save.".
  - Web error: Error: a Save Draft toast

- **Completer never sees day pay @p0** — notrun (account-hub/08-timecard-assigned.spec.js:232)
  - Where: /film-tools/timecard
  - Steps: Expand a filled day ("View breakdown").
  - Expected: "Breakdown" shows Hours / Basic / OT in hours with "This surface is not sent pay rates, so the breakdown is shown in hours rather than money…"; allowance and rental chips do show their amounts.

- **Submit the week for review @p0** — notrun (account-hub/08-timecard-assigned.spec.js:246)
  - Where: /film-tools/timecard
  - Steps: Click "Submit to <first name> for Review". → Review the modal, click "Confirm & Submit to <name>".
  - Expected: Modal "Submit to Crew for Review" with "Days completed", "Days incomplete" (or "All complete"), "Incomplete days are still submitted — …", "Your rates visible?" / "No — rates hidden from completer"; POST /review/<timecardId>/days/save-and-submit {user_id, timezone, days, submit_dates} with every pending/returned day except rest, out-of-range and future; those days show "Submitted for Review" and lock.

- **Review Inbox list @p0** — notrun (account-hub/08-timecard-assigned.spec.js:275)
  - Where: /film-tools/timecard
  - Steps: As Crew A click "Review Inbox" (badge shows count).
  - Expected: GET /review/inbox; one entry per week with "By <names>"; empty → "All caught up" / "No days are awaiting your review."; before picking: "Select a week to review".

- **Review a week @p0** — notrun (account-hub/08-timecard-assigned.spec.js:287)
  - Where: /film-tools/timecard
  - Steps: Select a week. → Click "Log in/out" on a day.
  - Expected: "<n days> awaiting your review", "completed by <names>", "Read-only" tag, "My Timecard"; day pay hidden but allowance amounts shown; per-day "Approve" / "Return"; opening clears the received_inbox badge. "Log in/out" shows the times with "Times this crew member's device recorded for the day." and "Close" only (no Replace).

- **Approve one day @p0** — notrun (account-hub/08-timecard-assigned.spec.js:311)
  - Where: /film-tools/timecard
  - Steps: Click "Approve" on a day.
  - Expected: POST /review/<id>/days/<dateMs>/approve (no confirm); day shows "Approved"; B sees it locked.

- **Return one day with a note @p0** — notrun (account-hub/08-timecard-assigned.spec.js:327)
  - Where: /film-tools/timecard
  - Steps: Click "Return" on a day. → Click "Return Day" empty, then with a note.
  - Expected: Modal "Return <day>" / "This day will be returned to the completer for correction." / "Return note (required)"; empty → "Please add a return note first."; POST …/return {return_note}; B sees "Returned by <name>: <note>" and can edit and resubmit.

- **Owner save after completer days @p1** — notrun (account-hub/08-timecard-assigned.spec.js:359)
  - Where: /film-tools/timecard
  - Steps: B submits days (under_review). → Crew A edits another day in Weekly (auto-save). → Reload Review Inbox / Assigned.
  - Expected: Expected: day review states and completer notes kept. Weekly's serializer omits assignment_status, notes, return_note; check whether they were reset.

- **Completer allowance edit on an in-flight week @p1** — notrun (account-hub/08-timecard-assigned.spec.js:385)
  - Where: /film-tools/timecard · account: B add an allowance
  - Steps: Crew A submits; T1 approves. → As B add an allowance on that week (auto-save).
  - Expected: Expected: a warning like Weekly's. Code sends PATCH /weekly/:id with no confirm; check whether T1's approval was wiped.

### Default chain (stubbed: the baseline chains stay as seeded)

- **Configure the default chain @p0** — notrun (account-hub/02-setup-approvers.spec.js:107)
  - Where: /film-tools/account-hub/approvers
  - Steps: "Configure". → Level 1 rule "Default". → "Add Users", pick user D, "Add 1 user". → "Save changes".
  - Expected: Builder intro "Configuring default approval levels for all departments…"; POST /account-hub/approval-tiers?module=purchase_orders with scope all and tiers: [{ order: 1, rules: [{ type: "default", user_ids: [D] }] }]; green "Approval levels saved successfully."; builder closes after about 1.2 s; card shows "L1" with D's avatar; button now "Edit".

- **The default chain cannot be emptied @p0 @smoke** — notrun (account-hub/02-setup-approvers.spec.js:136)
  - Where: /film-tools/account-hub/approvers
  - Steps: Edit the default chain, remove every user. → "Save changes".
  - Expected: Red "Please add at least one level."; no request.

- **"Amount greater than" needs a positive threshold @p0** — notrun (account-hub/02-setup-approvers.spec.js:152)
  - Where: /film-tools/account-hub/approvers
  - Steps: Add a rule "Amount greater than". → Leave the amount empty (then 0), save. → Enter 5000, save.
  - Expected: Note "Applicable for all currencies. No exchange rates applied."; empty/0 shows "Enter an amount greater than 0 for each "Amount greater than" rule." and blocks; 5000 saves { type: "amount", amount_threshold: 5000 }.

- **An empty level in the middle is removed after a confirm @p0** — notrun (account-hub/02-setup-approvers.spec.js:178)
  - Where: /film-tools/account-hub/approvers
  - Steps: Level 1 empty, Level 2 with D, Level 3 empty. → "Save changes". → Confirm.
  - Expected: Confirm "Empty approval levels" with "Level 1 has no approvers. It will be removed and the levels below will move up…"; saved body has one level order: 1 with D. Trailing empty levels are dropped with no confirm.

### Approver picker

- **The picker offers only users with view rights (plus Accounts) @p0 @smoke** — notrun (account-hub/02-setup-approvers.spec.js:206)
  - Where: /film-tools/account-hub/approvers
  - Steps: Open the picker on Purchase Orders (and on Invoices).
  - Expected: GET {projectBase}/v2/access/users for purchase_order_tool (Invoices uses the same tool); D (has rights) and Accounts members are offered; E (no rights) isn't.

### Department overrides

- **Give a department its own chain @p0** — notrun (account-hub/02-setup-approvers.spec.js:278)
  - Where: /film-tools/account-hub/approvers
  - Steps: Expand a department that uses the default, "Edit". → Save without changes.
  - Expected: Builder "Configuring approval levels for {department}. This overrides the default configuration." seeded with the default chain; POST with scope: "department" and department_id = the department _id; department now counts as Custom.

- **Revert a department to the default chain @p0** — notrun (account-hub/02-setup-approvers.spec.js:313)
  - Where: /film-tools/account-hub/approvers
  - Steps: Edit a custom department, remove every user. → Save, confirm.
  - Expected: Confirm "Remove all approvers?" ("…it will use the global (default) approvers…"); DELETE /account-hub/approval-tiers/{id}; "This department will now use the global approvers."; department back under Default.

### Chain takes effect (two users)

- **The default chain routes a PO to its approver @p0** — notrun (account-hub/02-setup-approvers.spec.js:341)
  - Where: /film-tools/account-hub/approvers
  - Steps: Set the PO default chain to L1 = D. → Crew raises and sends a PO for approval. → Log in as D.
  - Expected: The PO is in D's PO Approval Queue; after D approves it is fully approved.

### Codes: create, edit, deactivate, duplicates

- **Bulk add a top-level code (autosave) @p0** — notrun (account-hub/02-setup-coa.spec.js:114)
  - Where: 02 Setup and configuration
  - Steps: "New COA Entry". → Type code "e2e1001" and a name. → Wait 2 s.
  - Expected: Full-page "Add chart-of-accounts entries" with "Adding top-level entries — each row saves itself automatically once it has a code, no Save button needed."; POST /account-hub/chart-of-accounts { code: "E2E1001", line_type: "category", cost_type: "expense", parent_id: null, is_active: true, posting_box: true }; status "All changes saved".

- **Edit a manual code @p0** — notrun (account-hub/02-setup-coa.spec.js:149)
  - Where: 02 Setup and configuration
  - Steps: Hover a row, click "Edit". → Change "Display name", untick "Posting". → "Save changes".
  - Expected: Modal "Edit code · {code}" with "Line type", "Parent ({type})", "Nominal / Account code" disabled and "Code is immutable — clone to a new code if needed.", "Cost type", "Display name", "Active", "Posting"; PATCH /account-hub/chart-of-accounts/{id} { name, cost_type, is_active, posting_box }; chart reloads.

- **Deactivate a code @p0** — notrun (account-hub/02-setup-coa.spec.js:178)
  - Where: 02 Setup and configuration
  - Steps: Click the trash ("Deactivate") on an E2E code. → Confirm "Deactivate".
  - Expected: Confirm "Deactivate code" with "Deactivate "{code} · {name}"?"; button shows "Deactivating…"; DELETE /account-hub/chart-of-accounts/{id}; row shows "Inactive".

### Codes: duplicate rules

- **Duplicate code rules in bulk add @p0** — notrun (account-hub/02-setup-coa.spec.js:198)
  - Where: 02 Setup and configuration
  - Steps: Type an existing active code. → A retired (inactive) code. → The same new code in two rows. → A code saved then removed earlier in this session.
  - Expected: Red outline, no save, tooltip: "Code "X" already exists" / "Code "X" was previously used and retired — reactivate it from the Chart of Accounts list instead of creating it again" / "Code "X" is used by another row in this list" / "Code "X" was already used earlier in this session and can't be reused here". Case-insensitive.

### Layers

- **Create a layer @p0** — notrun (account-hub/02-setup-coa.spec.js:301)
  - Where: 02 Setup and configuration
  - Steps: "Layers" tab. → "New set". → Name "E2E<run> Locations", pick a colour, prefix "loc". → "Create set".
  - Expected: Intro "Layers — analytical dimensions"; "Create set" disabled until a name; POST /account-hub/tracking-sets { name, color, prefix: "LOC", active: true }; list reloads via GET /account-hub/tracking-sets?include_nodes=true. Blank prefix is omitted.

- **Add and edit a layer code @p0** — notrun (account-hub/02-setup-coa.spec.js:328)
  - Where: 02 Setup and configuration
  - Steps: Expand the layer, "Add code". → Code "LOC-LON", Label "London", description. → Save. → Edit it.
  - Expected: Save disabled until code and label; POST /account-hub/tracking-sets/{id}/nodes with parent_id: null, is_header: false; edit sends PATCH …/nodes/{nodeId}; card shows "{n} codes".

### Budget and import

- **Budget page lists versions @p0 @smoke** — notrun (account-hub/02-setup-coa.spec.js:392)
  - Where: 02 Setup and configuration
  - Steps: CONFIGURATION › "Budget".
  - Expected: Title "Budget"; "Budget Versions" from GET /account-hub/budgets ("{n} versions", " · 1 LIVE"); empty project shows "No budget versions yet" with "Import Budget".

- **Upload and parse a budget file @p0** — notrun (account-hub/02-setup-coa.spec.js:407)
  - Where: 02 Setup and configuration
  - Steps: "Import Budget". → Pick the CSV. → "Parse file".
  - Expected: "Upload a budget file", "PDF · XLSX · XLS · CSV — up to 20 MB"; "Parsing…"; S3 upload, then POST /account-hub/budgets/import:dry-run with the attachment; moves to the preview.

- **Import a budget (Append) @p0** — notrun (account-hub/02-setup-coa.spec.js:432)
  - Where: 02 Setup and configuration
  - Steps: "Import" with "Append".
  - Expected: "Importing…"; POST /account-hub/budgets/import:commit with coa_mode: "append"; the wizard closes and the list shows the new DRAFT version. No success toast appears (see defects).

### Forms Configuration

- **Rename a PO field; the PO form shows it @p0** — notrun (account-hub/02-setup-forms-vendors.spec.js:119)
  - Where: /film-tools/account-hub/form-config · account: D open a new PO
  - Steps: Rename a PO custom or header field. → "Save changes". → As D open a new PO.
  - Expected: POST /account-hub/form-templates?module=purchase_orders with { template: [...] }; banner "Form template saved successfully."; the PO form shows the new label.

- **Add a custom field (and a same-name twin) @p0** — notrun (account-hub/02-setup-forms-vendors.spec.js:163)
  - Where: /film-tools/account-hub/form-config
  - Steps: "+ Add Custom Field". → Name "Delivery Note #", type "Text". → Add it. → Add a second field with the same name.
  - Expected: Add button disabled until a name; type options Text, Number, URL, Email, Phone, Select, Date; stored label is delivery_note, the second delivery_note_2; both start optional and visible.

### Vendors

- **Vendor required fields @p0 @smoke** — notrun (account-hub/02-setup-forms-vendors.spec.js:302)
  - Where: /film-tools/account-hub/vendors
  - Steps: "Create Vendor" on an empty form. → Enter "bad@" as email.
  - Expected: Inline errors "Vendor name is required", "Contact person is required", "Email is required", "Address line 1 is required", "City is required", "Postal code is required", "Country is required"; "bad@" gives "Enter a valid email"; no POST.

- **Bank details need a holder and an account number or IBAN @p0 @smoke** — notrun (account-hub/02-setup-forms-vendors.spec.js:318)
  - Where: /film-tools/account-hub/vendors
  - Steps: Enter only "Bank Name". → Add "Account Holder Name". → Add only an "IBAN Code".
  - Expected: Errors "Account holder is required" and "Enter an account number or an IBAN" until satisfied; IBAN alone (no account number) passes.

- **Type-to-create a vendor from the PO form @p0** — notrun (account-hub/02-setup-forms-vendors.spec.js:505)
  - Where: /film-tools/account-hub/vendors
  - Steps: As D on a new PO type "E2E <run> Newco" in the vendor picker. → Pick "Create "E2E <run> Newco"". → Save the PO.
  - Expected: Picker shows "E2E <run> Newco (new)" as Non-Verified with no request; on save POST /api/v2/vendors { name } then the PO with the real vendor id; the register lists it as Non-Verified with no email.

### Vendors › Accountant vendor: create, edit, delete

- **Accountant creates a vendor (Verified) @p0** — notrun (account-hub/02-setup-forms-vendors.spec.js:365)
  - Where: /film-tools/account-hub/vendors
  - Steps: "Add Vendor". → Fill "Vendor / Company Name", "Contact Person", "Email", "Address Line 1", "City", "Postal / ZIP Code", "Country". → "Create Vendor".
  - Expected: Form "New Vendor" with banner "Vendors created here are automatically marked Verified and available for PO assignment immediately…"; POST /api/v2/vendors; success toast; the vendor appears with "Verified".

- **Edit a Verified vendor @p0** — notrun (account-hub/02-setup-forms-vendors.spec.js:387)
  - Where: /film-tools/account-hub/vendors
  - Steps: As A click "Edit vendor" on a Verified vendor. → Change the contact, "Save Changes".
  - Expected: "Edit Vendor Details" with "Update the vendor information below. Changes are saved immediately."; PATCH /api/v2/vendors/{id}; toast; list refreshes.

- **Delete a vendor @p0** — notrun (account-hub/02-setup-forms-vendors.spec.js:409)
  - Where: /film-tools/account-hub/vendors
  - Steps: Click "Delete vendor" on an E2E vendor. → Confirm.
  - Expected: Confirm "Delete Vendor" with "Are you sure you want to delete "{name}"? This action cannot be undone."; DELETE /api/v2/vendors/{id}; toast; row gone.

### Vendors › Non-Verified vendors (crew-created)

- **Accountant edits a Non-Verified vendor: Save & Verify @p0** — notrun (account-hub/02-setup-forms-vendors.spec.js:448)
  - Where: /film-tools/account-hub/vendors
  - Steps: As A edit D's Non-Verified vendor.
  - Expected: Only "Save & Verify" (no "Save Changes"); click sends PATCH then POST /api/v2/vendors/{id}/verify; vendor now "Verified". If the PATCH fails, no verify is sent.

- **Detail modal: masked bank details and Mark Verified @p0** — notrun (account-hub/02-setup-forms-vendors.spec.js:475)
  - Where: /film-tools/account-hub/vendors
  - Steps: Click a Non-Verified vendor row. → "Reveal" bank details, wait 5 s. → "Mark Verified".
  - Expected: Modal shows contact, address, masked bank details, "Added by", "Verified by", "Last updated"; Reveal re-masks after 5 s; "Edit" and "Mark Verified" show only to accountants on Non-Verified vendors; "Verifying…" then POST …/verify, modal closes, list refreshes.

### Posted lines and their asset rows (two users)

- **Only posted and closed PO lines appear @p0** — notrun (account-hub/03-asset-register.spec.js:220)
  - Where: /film-tools/asset-report
  - Steps: Note an approved-not-posted PO's line. → Post that PO (accountant). → Reload the register.
  - Expected: Before posting the line is absent; after posting it appears; closed PO lines stay listed.

- **Asset Register Rules narrow the list @p0** — notrun (account-hub/03-asset-register.spec.js:271)
  - Where: /film-tools/asset-report
  - Steps: Set Expenditure type "Purchase" in PO Settings → reload. → Set price range 100–500 → reload. → Add a tag → reload.
  - Expected: Step 1 only Purchase lines; step 2 only lines whose total is 100–500 inclusive; step 3 only lines carrying that tag. "All" brings Rent and Consumption back (Rent qualifies only under "All"). Restore the rule.

- **Open a line @p0** — notrun (account-hub/03-asset-register.spec.js:330)
  - Where: /film-tools/asset-report
  - Steps: Open the register. → Click a row.
  - Expected: Full-page detail ("Asset Register" breadcrumb, back "Back to Asset Register"); Details: Vendor, Department, Expense Type, Qty, Unit Cost, Total; category cards, note and uploader disabled with skeleton tiles until GET /purchase-orders/asset-register?line_item_id=<line> (or GET …/asset-register/:asset_id) returns; URL unchanged.

- **First save creates the asset row @p0** — notrun (account-hub/03-asset-register.spec.js:354)
  - Where: /film-tools/asset-report · account: A1
  - Steps: As A1 open a line with no asset row ("Not saved yet"). → Click "Keep". → Click header "Save".
  - Expected: POST /purchase-orders/asset-register { entity: "purchase_order", po_id, line_item_id, category: "Keep", attachments: [], comments }; header button reads "Saved"; list row shows the "Keep" pill.

- **Change and clear the category @p0** — notrun (account-hub/03-asset-register.spec.js:377)
  - Where: /film-tools/asset-report
  - Steps: On that line click "Sell" → "Save". → Click "Sell" again (un-pick) → "Save".
  - Expected: Step 1 PATCH /purchase-orders/asset-register/:id { category: "Sell", attachments }; pill "Sell". Step 2 saves category: ""; pill disappears.

- **Save a note on an existing row @p0** — notrun (account-hub/03-asset-register.spec.js:408)
  - Where: /film-tools/asset-report
  - Steps: Type in "Comments" ("Add a note about this asset…"). → Click the note's "Save". → Clear it and save.
  - Expected: Meta shows "Unsaved changes" while typing; PATCH …/asset-register/:id/comment { comments }; "Saved" flashes; meta "Last saved by <Name · Designation> · <date>". Clearing sends "" and the stamp is removed.

- **View-only user cannot edit @p0** — notrun (account-hub/03-asset-register.spec.js:444)
  - Where: /film-tools/asset-report · account: A3
  - Steps: Log in as A3. → Open a line that has a note and files.
  - Expected: Keep/Sell disabled, note read-only, no Save buttons, no upload tile and no × on files; files still open in the viewer.

### Access & navigation — accountant console

- **Assistant accountant console @p0** — notrun (account-hub/03-purchase-orders.spec.js:177)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: Log in as U5. → Open the Account Hub tile.
  - Expected: Lands on /purchase-orders/queue/my; tabs "All POs", "Queue", "PO Entry", "Reports"; no "Posted", no "Settings"; "Enter PO" button; banner "Assistant View — Some sections are hidden based on your role…".

- **Console rows gated by assignment / approver role @p0** — notrun (account-hub/03-purchase-orders.spec.js:224)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, open "All POs". → Click a PO assigned to U6 where U5 is not an approver. → Click a PO assigned to U5.
  - Expected: Step 2 row is dimmed and nothing opens; step 3 opens PODetailModal. U6 can open every row.

### Lifecycle A: raise, approve, process, post, close (two users)

- **Crew raises a PO @p0** — notrun (account-hub/03-purchase-orders.spec.js:289)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, click "Create PO". → Pick vendor V1, department Dept A, description, one line (description, qty 2, unit price 50). → Click "Create & Submit PO".
  - Expected: Topbar tag "Pending Approval", banner "Department PO Request."; POST /purchase-orders with status: "PENDING", department_id = Dept A, approvals array; server toast; lands on /purchase-orders/my with the PO showing "Pending (0/2)".

- **No approve buttons for non-approvers @p0** — notrun (account-hub/03-purchase-orders.spec.js:315)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: Log in as U1. → Open own pending PO in "My POs".
  - Expected: No "Approve"/"Reject"; footer has "History", "View PDF", "Query", Edit and delete chips.

- **Approval by the Default approver (one-level baseline chain) @p0** — notrun (account-hub/03-purchase-orders.spec.js:329)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U1 submits a Dept A PO of 100. → U3: "Approval Queue" → open it → "Approve". → U4 approves.
  - Expected: Step 2 POST /purchase-orders/:id/approve { tier_number: 1, total_tiers: 2 }, server toast, modal closes, queue refetches silently, pill "Pending (1/2)". Step 3 tier 2 → status "Approved" in U1's My POs.

- **No Edit once fully approved @p0** — notrun (account-hub/03-purchase-orders.spec.js:343)
  - Where: /film-tools/account-hub/purchase-orders · account: creator
  - Steps: Log in as the creator. → Open an approved, an acct-entered and a posted PO in turn.
  - Expected: No "Edit" button on any (amendments paused, AMENDMENTS_ENABLED = false).

- **Reassign from the list (PA/FC) @p0** — notrun (account-hub/03-purchase-orders.spec.js:368)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: As U6, All POs → click an assignee chip. → Try "Reassign" without a reason. → Pick "Other (custom reason)", type text, submit.
  - Expected: Modal "Reassign <PO>", "Currently assigned to: <name>", options "Complex PO - Needs Senior Review", "Workload Balancing", "Department Enterprise", "Holiday/Absence Cover", "Accountant Escalation", "Other (custom reason)" with a /500 counter; button disabled until user + reason; PATCH sent; chip tooltip "Reason: <text>".

- **Open the Process page @p0** — notrun (account-hub/03-purchase-orders.spec.js:428)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, open an approved PO assigned to U5. → Click "Process".
  - Expected: Button shows "Processing…" while GET /purchase-orders/:id loads; URL /purchase-orders/po-entry; breadcrumb "Purchase Orders / Process · <PO>"; descriptions upper-cased; uncoded lines seeded with the header nominal (or 2100). Back arrow "Back to Queue" returns to the list it came from.

- **Coded total must match the PO total @p0** — notrun (account-hub/03-purchase-orders.spec.js:450)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Change a line amount so the ledger total is off by more than 1p. → Click "Save". → Bring it within 1p and save.
  - Expected: Footer shows "PO Total", "Ledger Total", "Difference" with "over"/"under" and "Query" appears; Save → alert "Coded lines must match the PO total before you can save", no PATCH. Within 1p saves.

- **Code lines and save @p0** — notrun (account-hub/03-purchase-orders.spec.js:477)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Set codes with the code picker, Exp. Type ("Purchase", "Consumables", "Rent"), "Tags", "Layers". → Type a code not in the chart. → Click "Save".
  - Expected: "Saving…" then PATCH /purchase-orders/:id with line_items (unknown code sent as [[code]]) and header fields, then GET /:id; one server toast; values persist on reload.

- **Post blocked by an uncoded line @p0** — notrun (account-hub/03-purchase-orders.spec.js:516)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Blank line 2's code (keep its amount). → Click "Post to Ledger".
  - Expected: Alert "Line 2 needs a nominal code before you can post"; row 2 painted red; tooltip "Every line needs a nominal code before you can post"; no POST. Save still works.

- **Post to ledger @p0** — notrun (account-hub/03-purchase-orders.spec.js:539)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open a balanced, fully coded PO with an effective date on the Process page. → Click "Post to Ledger".
  - Expected: "Posting…" then POST /purchase-orders/:id/post (effectiveDate epoch, lineItems, poDetails); toast; navigates to /purchase-orders/posted; row "Open".

- **Close one PO @p0** — notrun (account-hub/03-purchase-orders.spec.js:572)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open row → "Close". → Try "Close PO" before ticking. → Add a reason, tick "I confirm I want to close <PO>", click "Close PO".
  - Expected: Modal "Close Purchase Order" with Vendor, Amount, Remaining, Current Status, "Effective Closing Date" (defaults to the PO's date, min lock + 1); disabled until ticked; POST /purchase-orders/:id/close { reason, effective_date }; row "Closed"; expanded row "Closure Reason" (or "No reason provided").

- **Console delete rights (PA/FC deletes a crew PO) @p0** — notrun (account-hub/03-purchase-orders.spec.js:604)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: As U6, delete a PO raised by U1. → As U5, open a PO assigned to U5 and one assigned to U6. → Repeat in a locked period.
  - Expected: U6 and the assignee U5 see the delete chip (and "Delete" on the Process page); U5 does not on U6's PO; delete stays available in a locked period.

### Submit & approval levels

- **Tier 2 waits for tier 1 @p0** — notrun (account-hub/03-purchase-orders.spec.js:1013)
  - Where: /film-tools/account-hub/purchase-orders · account: U4
  - Steps: Submit a fresh PO. → As U4, open it.
  - Expected: U4 has no Approve/Reject until U3 approves tier 1 (nextTier = 1).

### Who sees Process / Send to Vendor (status matrix, PA/FC)

- **Who sees Process @p0 @smoke** — notrun (account-hub/03-purchase-orders.spec.js:1088)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5 open an approved PO assigned to U5, one assigned to U6, a pending one and a closed one. → Repeat as U6.
  - Expected: U5: "Process" only on its own approved PO. U6: on approved, acct-entered and posted POs; never on pending, draft, rejected or closed (detail modal); unassigned POs only for U6.

- **Who and when Send to Vendor shows @p0 @smoke** — notrun (account-hub/03-purchase-orders.spec.js:1099)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: Check pending, rejected, approved, acct-entered, posted and closed POs as U6. → As U5 on a PO not assigned to U5. → Any PO in the crew view.
  - Expected: Shown for approved, acct-entered, posted and closed (if not yet emailed); never for pending/rejected/draft, non-assignee assistants, or in the crew view.

- **No posting limit @p0** — notrun (account-hub/03-purchase-orders.spec.js:1126)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: Make U5 the assignee of a 100,000 PO (no other rights). → As U5, process and post it.
  - Expected: Post succeeds; no senior or limit check in the UI.

- **Non-assignee cannot reach Post @p0** — notrun (account-hub/03-purchase-orders.spec.js:1130)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: Log in as U5. → Open an approved PO assigned to U6, then an unassigned one.
  - Expected: No "Process" → no way to post. An unassigned PO is processable only by U6.

### Send to vendor

- **Send from the detail modal @p0** — notrun (account-hub/03-purchase-orders.spec.js:1137)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, open an approved, coded PO assigned to U5. → Click "Send to Vendor".
  - Expected: No confirm; "Sending…" then POST /purchase-orders/:id/send-vendor-email; toast; button disappears; "Emailed To Vendor" block shows the sender and "<date> at <time>". Vendor V1 receives the PDF + T&C.

### Posted tab and Close Off Period

- **Relief labels on the Posted tab (stubbed rows) @p0 @smoke** — notrun (account-hub/03-purchase-orders.spec.js:1173)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub GET /purchase-orders with posted POs where paid_amount = 0, half, ≥ gross, plus a CLOSED one. → Open "Posted".
  - Expected: Status "Open", "Partially Relieved", "Fully Relieved", "Closed"; "Relieved" and "Remaining" columns (dash when 0); "Close" only on Open and Partially Relieved rows; header "Posted Ledger" count and "Total posted: <total>".

- **Close Off Period (list narrowed to this run's PO) @p0** — notrun (account-hub/03-purchase-orders.spec.js:1205)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: As U6, Posted → "Close Off Period" on the group header. → Tick "I confirm I want to close off <n> POs", click "Close Off Period".
  - Expected: Modal "Close Off Unknown", "<n> POs will be closed", date defaults to the first unlocked day; POST /purchase-orders/bulk-close { po_ids, effective_date } (UTC midnight); includes Fully Relieved, skips closed and locked rows. Button disabled with tooltip "All POs in this period are already closed." when nothing is left.

### Settings & team rights

- **Auto-assignment rule assigns on full approval @p0** — notrun (account-hub/03-purchase-orders.spec.js:1254)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: "Add Rule"; Assign to U5, Departments Dept A; "Save". → U1 submits a Dept A PO; U3 and U4 approve.
  - Expected: POST /account-hub/assignment-rules (module purchase_orders); rule summary "1 dept"; after full approval the PO shows assignee U5 ("Auto-assigned · Dept A Dept Rule" on the Process page) and appears in U5's My Queue.

### 

- **J1 Commit → actual → paid, invoice leg through the UI @journey** — notrun (account-hub/04-invoices-journey.spec.js:40)

### Payment runs

- **Run access gates operations @p0** — notrun (account-hub/04-invoices-payments.spec.js:98)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-JR (no "Authorise Runs") opens Open Items, Wires and an active run. → ACC-PA (no team row) does the same.
  - Expected: ACC-JR: "Create BACs Run", "Process", "Mark Paid", "Cancel run" disabled with tooltip "You don't have access to authorise payment runs."; ACC-PA: enabled.

- **Create BACs Run @p0** — notrun (account-hub/04-invoices-payments.spec.js:115)
  - Where: /film-tools/account-hub/invoices
  - Steps: With only BACS ticked, click "Create BACs Run".
  - Expected: "Creating…"; POST /api/v2/invoices/active-runs/batch { pay_method: "bacs", all: true, exclude_invoice_ids }; switches to "Active Runs" and opens the last run created (PR-###, pending); those invoices leave Open Items.

- **Mark wires paid @p0** — notrun (account-hub/04-invoices-payments.spec.js:137)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Wires" › a row's "Mark Paid". → Tick 2 rows › bar "Mark Paid".
  - Expected: POST /api/v2/invoices/payment-runs/mark-paid { method: "wire_faster", invoice_ids }; rows move to "Recently Marked as Paid" with "Paid" (GET …/payment-runs/recently-paid); status paid.

- **Reject a run @p0** — notrun (account-hub/04-invoices-payments.spec.js:234)
  - Where: /film-tools/account-hub/invoices
  - Steps: A run approver › "Reject". → Empty reason, then a reason › "Reject run".
  - Expected: "Reject {number}" / "Please provide a reason for rejecting this payment run. This is mandatory."; button disabled until a reason; POST /active-runs/:id/reject { reason }; run shows "Run rejected" with reason and who (no per-level chain); its invoices return to Open Items.

### Settings & team rights

- **Add a team member; Is Senior @p0** — notrun (account-hub/04-invoices-payments.spec.js:291)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Add Member". → Pick a user in "Team Member", Posting Limit 1,000, "Can authorise payment runs" on › "Add Member". → Edit another member, switch "Is Senior" on › "Save Changes".
  - Expected: Each save is an immediate PATCH /api/v2/invoices/settings { team_members } (whole array); Is Senior turns on and locks "Unlimited", "Can authorise payment runs", "Can override approvals" and saves posting_limit: null, run_access: true, override_access: true, is_senior: true.

- **Rights reach other users live @p0** — notrun (account-hub/04-invoices-payments.spec.js:336)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-JR has an Entry invoice open (no Post). → ACC-PA gives ACC-JR Posting Limit 500 and run access.
  - Expected: Without a reload ACC-JR sees "Post to Ledger" and enabled Payment Runs buttons (invoice:settings:updated → refetch).

### Receipts and out-of-pocket claims

- **No submittable float @p0 @smoke** — notrun (account-hub/06-petty-cash-claims.spec.js:98)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW whose floats are Awaiting Approval, Approved or Ready to Collect opens "Submit Receipts".
  - Expected: "Loading…" then "No active float" / "You don't have an active float, so you can't submit receipts…"; no form.

- **Petty-cash batch within the balance @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:117)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW (Collected float, balance 200) fills one receipt: "Drop or click" image, Date of Purchase, Amount 50, Vendor, Category. → "Submit Claim for Coding & Approval".
  - Expected: Card "Reduce My Float" / "Batch is within float balance — this will reduce your remaining float." / "£50.00 will reduce the float"; file uploads first; POST /claims {expense_type:"pc", float_request_id, currency: the float's, settlement_type:"REDUCE_FLOAT", settlement_details:{follow_up:null}, claims:[{description, category, gross_amount, receipt_date, attachment}]}; toast; lands on "Receipts History" with the batch.

- **Overdraw the float → reimburse @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:158)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Same float, receipts totalling 260. → Fill Account Name, Sort Code, Account Number and one row via "Add additional detail (IBAN, BIC, routing number, etc.)". → Submit. → Repeat with a total exactly equal to what is available.
  - Expected: "Reimburse Me" / "This batch overdraws your float. The overdraft will be reimbursed to you." / "Overdraft £60.00 reimbursed · £200.00 float consumed"; payload settlement_type:"REIMBURSE", payment_method:"BACS", bank_details with additional_details (untitled rows dropped). 4 stays "Reduce My Float".

- **Submit an out-of-pocket claim @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:201)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW → "Out of Pocket" → "Submit Receipts". → Pick "Currency *", add a PDF receipt and bank details. → "Submit OOP Claim for Coding & Approval".
  - Expected: Banner "Out-of-Pocket — <name>"; one batch currency picker, no settlement card; POST /claims {expense_type:"oop", settlement_type:"REIMBURSE", currency, settlement_details:{payment_method,…}}; lands on OOP "Receipts History".

### Validation

- **Receipt rules and their order @p0 @smoke** — notrun (account-hub/06-petty-cash-claims.spec.js:234)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Submit with Vendor blank. → Fill Vendor, leave Date of Purchase blank. → Fill the date, no file.
  - Expected: In order: "Each receipt must have a description and amount." → "Each receipt must have a date of purchase." → "Each receipt must have an attachment — upload the receipt image or PDF."; file input accepts image/*,.pdf. Same on OOP.

### Coordinator review

- **Coding Queue list @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:291)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Coding required for D1. → CREW submits a batch. → COORD → "Coding Queue".
  - Expected: Notice "Coding Queue — <name>"; "{n} pending"; filters All, Petty Cash, Out of Pocket; row status "Needs Coding" (Pending) or "Coding", quoted batch notes; GET /claims/coding-queue. Without coding the batch goes straight to Audit Queue.

- **Coding view locks claimant fields @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:309)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Open a batch.
  - Expected: Full page "Petty Cash / Coding Queue / #ref"; receipt name, Amount and Category render as locked fields (tooltip "Locked — entered by claimant"); Purchase Date read-only; Vendor and Cost Code editable; no Effective Date, Assign/Reassign or Post; buttons "History", "Save Draft", "Forward to Accounts".

- **Forward to Accounts @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:330)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: With one receipt uncoded, check the button. → Code all, click "Forward to Accounts".
  - Expected: Disabled until every receipt is coded; POST …/save-and-submit; batch leaves the queue and appears in Audit Queue as "In audit".

### Accountant audit

- **Only the assignee or a senior opens a batch @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:359)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_LIMIT views batches assigned to ACCT_POSTER in Audit Queue and in Post & Ledger. → ACCT_POSTER views an unassigned batch in each. → SENIOR_PA views the same rows.
  - Expected: 1–2: row dimmed with a lock, no "Open" / "Review". 3: every row opens.

- **Verify receipts @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:386)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Open a batch, click "Verify" on a receipt. → Click "Verified".
  - Expected: POST …/save-claims with is_verified toggled; button flips "Verify" ↔ "Verified".

- **Send for Approval needs coding @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:409)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Leave a receipt uncoded, click "Send for Approval".
  - Expected: "Assign a cost code to every receipt before sending this batch for approval" (or "{n} lines need a nominal code before this batch can go for approval" with lines in red); no request.

- **Send for Approval needs every receipt verified @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:425)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Code all, verify one of two, click "Send for Approval".
  - Expected: "Click Verify on every receipt before sending this batch for approval (1 still to verify)"; button stays clickable.

- **Send for Approval @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:446)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Code and verify all, click "Send for Approval".
  - Expected: POST …/save-and-verify; back on the queue without the batch; it appears in the Approval Queue as "Awaiting Approval".

### Submit and approval levels (receipt batches)

- **Approve a whole batch @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:472)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Open the batch. → Keep all ticked ("Select All", "n of n selected"), "Approve".
  - Expected: POST /claims/:id/batch-approval {action:"approve", claim_ids:null, tier_number, total_tiers}; modal closes; queue reloads; at the last level the batch is READY_TO_POST and shows in Post & Ledger. The modal has no "Query" button.

- **Batch two-level approval @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:496)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: D1 has two levels. → APPROVER1 approves; APPROVER2 approves.
  - Expected: After level 1 it stays queued for APPROVER2; after level 2 it is Ready to Post.

- **Override a batch @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:528)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "Accountant Override — Receipt Batches" on; ACCT_POSTER has Can Override and isn't the next approver. → Row "Override" (or in the modal).
  - Expected: POST /claims/:id/override; batch moves to Post & Ledger with pill "Override" for accountants; CREW's history shows "Ready to Post".

### Query / reject / resubmit

- **Reject a whole batch @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:571)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Open a batch, "Reject". → Type a reason, "Confirm Rejection".
  - Expected: Modal "Reject Entire Batch" / "This will reject the entire batch and send it back to the crew member."; confirm disabled until a reason; {action:"reject", claim_ids:null, reason}; CREW: filter "Rejected" lists it; the receipt detail shows the reason and "Rejected by <name>".

### Assignment

- **First-time assignment @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:610)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: SENIOR_PA opens an unassigned batch → "Assign". → Pick self, "Assign".
  - Expected: Modal "Assign #ref"; "Assign To *" lists the accounts team including self; no reason field; POST /claims/:id/assign {assigned_to, assignment_reason:null}; header shows the assignee.

- **Reassign needs a reason @p0** — notrun (account-hub/06-petty-cash-claims.spec.js:634)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: On an assigned batch → "Reassign". → Pick a user, try to submit; choose "Other (custom reason)" and type text. → Submit.
  - Expected: "Currently assigned to <name> (<designation>)"; current assignee not offered; "Reason *" options Complex batch — needs senior review, Workload balancing, Department expertise, Holiday / absence cover, Accountant escalation, Other (custom reason); custom text max 500 with counter; submit disabled until user and reason; assignment_reason sent; Post & Ledger row stays with the new owner.

### Floats — request and form fields

- **Form follows Forms Configuration @p0 @smoke** — notrun (account-hub/06-petty-cash-floats.spec.js:111)
  - Where: /film-tools/account-hub/cash-expenses · account: CREW
  - Steps: Forms Configuration → Petty Cash float form: hide one field, make a custom text field required, set Purpose optional, save. → As CREW open "New Float".
  - Expected: Hidden field absent; required fields show "*", optional custom fields show "(optional)"; Purpose still shows "*" (locked); Start Date and Collect Time never render even if present in the template; fields follow template order.

- **Crew submits a float request @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:156)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Fill Requested Amount, Purpose, other required fields and a future collect date. → Click "Submit float request".
  - Expected: POST …/cash-expenses/float-requests with req_amount, currency, purpose, collect_date (epoch ms) and select defaults; server success toast; view shows the flow chain (You done, Approval active) and "Float Details" with badge "Awaiting Approval", "Ref." and "Submitted On".

- **Accountant raises a float for crew @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:187)
  - Where: /film-tools/account-hub/cash-expenses · account: ACCT_POSTER
  - Steps: As ACCT_POSTER open "New Float". → Pick CREW in "Select crew member…", fill, submit. → Click "Cancel".
  - Expected: Department auto-fills from the picked user; payload has target_user_id; the form resets and stays open; float appears in CREW's "My Floats"; Cancel returns to Active Floats.

- **Request cap — flat maximum @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:218)
  - Where: /film-tools/account-hub/cash-expenses · account: CREW enter 500
  - Steps: Settings → Request Cap on, Max amount 500, Save. → As CREW enter 500, submit; then 501.
  - Expected: Helper "Maximum £500 — project cap."; 500 submits; 501 shows "Requested Amount exceeds the project cap of £500." and nothing is sent.

### Validation

- **Float amount must be above 0 @p0 @smoke** — notrun (account-hub/06-petty-cash-floats.spec.js:245)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Leave Requested Amount blank or 0, submit.
  - Expected: "Requested Amount must be greater than 0."; no request.

- **Required template fields @p0 @smoke** — notrun (account-hub/06-petty-cash-floats.spec.js:260)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Leave Purpose and a required custom field empty, submit.
  - Expected: Each shows "<Field> is required"; banner "Please fix the highlighted fields."; no request.

### Floats — approval

- **Approve a float (one level) @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:285)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW submits a float. → APPROVER1 → "Approval Queue" → "Float Requests" card. → Click the row to view it, close, then "Approve" on the row.
  - Expected: Card "{n} pending approval"; row: name, "Float request" chip, "#ref", quoted purpose, amount, "Pending 0/1". Detail shows "Requested Amount", "Purpose / Justification", "Approval Progress (0/1)", "History". POST /float-requests/:id/approve {tier_number:1, total_tiers:1}; row dims "Approved"; CREW's float shows "Approved".

- **Two approval levels @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:321)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: D1 has levels 1 and → 2. APPROVER1 approves. → APPROVER2 approves.
  - Expected: After 2 the row stays for APPROVER2 as "Pending 1/2" with an approver chip "Level 1"; APPROVER1 has no buttons. After 3 the float is APPROVED.

- **Reject a float with a reason @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:365)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: APPROVER1 clicks "Reject" on the row (opens "<ref> — Float Request"). → Footer "Reject". → Type a reason, "Confirm Rejection".
  - Expected: Modal "Reject <ref> — Float Request", hint "This will reject the float request and send it back to the crew member."; "Confirm Rejection" disabled while empty or whitespace; POST …/reject {reason}; modal closes; CREW sees "Rejected" and the flow chain marks Approval "Rejected".

- **Accountant overrides a float @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:401)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Settings: "Accountant Override — Float Requests" on; ACCT_POSTER has Can Override and isn't in D1's levels. → ACCT_POSTER → Approval Queue → "Override" on the row (repeat on another float from the modal).
  - Expected: POST /float-requests/:id/override (no body); row "Approved"; float ACCT_OVERRIDE labelled "Approved"; Active Floats offers "Ready to Collect…".

### Floats — issue and balance (Active Floats)

- **Ready to Collect with company and BS code @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:440)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: On an Approved row click "Ready to Collect…". → Try to confirm with no company; pick a company and enter a BS code. → "Confirm & Ready to Collect".
  - Expected: Modal "Set Company & BS Code"; "Confirm & Ready to Collect" disabled until a company is picked; POST /float-requests/:id/ready-to-collect {company_id, bs_code} (bs_code omitted when blank); pill "Ready to Collect", strip "<amt> ready for collection at production office.", button "Mark Collected".

- **Ready to Collect blocked without a custodian account @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:472)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Clear Float Custodian Account in Settings. → ACCT_POSTER opens "Ready to Collect…". → SENIOR_PA does the same → "Set up custodian account" → enter an account → "Save custodian account".
  - Expected: Non-senior: "No Float Custodian Account is set up yet. Ask a senior accountant to set the Float Custodian Account in Settings first.", confirm disabled. Senior: setup modal (Float Custodian Account *, BS Code From, BS Code To); after PATCH /settings the warning clears and confirm enables.

- **Mark Collected @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:508)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: On a Ready to Collect row click "Mark Collected".
  - Expected: POST …/collect; pill "Collected"; tiles "Issued", "Spent / Returned", "Balance" and "Spent <x> of <y>"; CREW can now use Submit Receipts and Cash Extension.

- **Coordinator's Active Floats is read-only @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:535)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: COORD opens "Active Floats", expands a float, opens its detail.
  - Expected: Search only (no "Funds", "Record Cash Return", "New Float"); no lifecycle buttons; detail without BS edit or Return to Approval.

### Top-ups and returns

- **Crew requests a top-up @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:578)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW (Collected float) → "Cash Extension" → "Request Top-up". → Enter only Amount; then Reason. → "Send Request".
  - Expected: Header "Top-ups" with "Float <ref> — <purpose>" (a "Select float" dropdown when several); modal "Requests a balance top-up on Float <ref>…"; "Send Request" disabled until amount above 0 and a reason; POST /float-requests/:id/request-top-up {amount, reason}; row "Pending".

- **Accountant marks a top-up done @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:615)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER → "Top-Ups". → On a pending card with enough room click "Mark topped up".
  - Expected: Card: "Current balance", "Issued float", "Spent", "Top-up amount", note; PATCH /top-ups/:id/complete; "Top-up marked as topped up"; card moves to "Recently settled" as "Topped up"; CREW's row updates.

- **Top-up above the room is refused @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:639)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: A pending top-up bigger than float limit − current balance → "Mark topped up".
  - Expected: Alert "Top-up exceeds float limit" / "This top-up is <amt> but only <room> of room remains on the float (limit − current balance)…"; no request.

- **Partial top-up @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:661)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "Partial top-up". → Try without a note; amount 0; amount above the room. → Valid amount and note → "Submit Partial Top-Up".
  - Expected: Modal "Partial Top-Up", "Actual amount topped up" prefilled, "Note *"; submit disabled without a note; 0 → "Partial amount must be greater than 0"; above room → alert "…exceeds the <room> of room left on the float…"; valid → PATCH …/partial {amount, note}, "Partial top-up recorded", settled pill "Partial".

- **Record a full cash return @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:711)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Active Floats → "Record Cash Return". → Pick a float, Amount = balance, Date Received, "Float closing — full return", Notes. → "Record Cash Return".
  - Expected: Modal "Record Manual Cash Return"; options "<name> · Float #<n> · Balance: <b>" for Collected, Active, Spending, Spent and Pending Return floats; POST /float-requests/:id/record-return {return_amount, received_date, return_reason:"close_full_return", reason_notes}; row updates.

- **Cash return rules @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:741)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: No float. → Amount 0. 3. Amount above balance. 4. "Float cancelled" or "Overspend settlement — crew paying back" with a partial amount. 5. "Partial return — float continues" with the full balance. 6. "Other" → "Close the float" / "Continue the float".
  - Expected: 1 "Please select a crew member / float."; 2 "Please enter a valid return amount." ("Record Cash Return" disabled while empty); 3 "Return amount <a> exceeds the remaining balance of <b>."; 4 "This option will close the float. The return amount must be the full remaining balance of <b>."; 5 "Returning the full balance of <b> will leave the float with zero balance. Choose a close option instead, or reduce the return amount."; 6 sends close_other / continue_other.

- **Close a spent float @p0** — notrun (account-hub/06-petty-cash-floats.spec.js:811)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: On a Fully Spent row click "Close Float".
  - Expected: POST …/mark-closed; pill "Closed"; summary "Float Limit" and "Returned".

### Posting and seniority / posting limits / senior sign-off

- **Post a batch @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:77)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER opens an assigned Ready to Post batch. → Check Effective Date; click "Post to Ledger".
  - Expected: Effective Date defaults to the first unlocked day up to today; POST /claims/:id/post {claims, effective_date}; batch leaves the list and shows under "History" as "Posted".

- **Coded total must match the gross @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:106)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Split lines so their total differs from the gross. → "Save", then "Post to Ledger".
  - Expected: "Coded lines must match the batch total before you can save" / "…before you can post"; no request; "Batch Gross" bar shows the gap.

- **Every line needs a nominal @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:132)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Leave a split line without a nominal, "Post to Ledger".
  - Expected: "1 line need a nominal code before you can post" (or "{n} lines need…"); lines in red; Save still allowed.

- **Who gets "Post to Ledger" @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:160)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Senior Sign-off Required off. → Open the same batch as SENIOR_PA, ACCT_POSTER, ACCT_LIMIT, ACCT_ZERO, ACCT_NOTEAM (assign as needed).
  - Expected: Post shown for SENIOR_PA (not in the team), ACCT_POSTER (Unlimited) and ACCT_LIMIT; ACCT_ZERO and ACCT_NOTEAM get no action at all. Header pill "Ready to post" vs "Coding" follows the same rule.

- **Posting limit ignores the amount @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:195)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_LIMIT (Settings shows "£100 · Above £100 Submit to Senior") opens a £500 batch.
  - Expected: "Post to Ledger" offered and the post goes through from the browser; confirm whether the server refuses.

- **Escalate (sign-off on) @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:214)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "Senior Sign-off Required" on. → ACCT_POSTER opens a batch: "Post to Ledger" and "Escalate" shown. → "Escalate", type a reason, "Confirm Escalation".
  - Expected: Modal "Escalate #ref to Senior" / "This batch will be flagged for senior accountant review…"; confirm disabled until a reason; POST …/escalate {escalation_reason}; batch leaves Post & Ledger; Sign-off shows "Note from <name> (<designation>):" and the reason.

- **Submit for Review (non-poster) @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:249)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Sign-off on; ACCT_ZERO opens the batch. → "Submit for Review".
  - Expected: "Submit for Review" and "Escalate", no "Post to Ledger"; POST …/submit-for-review; status UNDER_REVIEW shown "Under review" in Post & Ledger and listed in Sign-off.

- **Approve & Post from Sign-off @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:272)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Expand a batch, add "Senior Sign-off Notes". → "Approve & Post to Ledger" with no date; then pick a date and repeat.
  - Expected: No date: toast "Select an effective date before posting to the ledger."; then POST …/post {senior_notes, effective_date}; row removed; batch Posted.

- **Posting outcomes @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:302)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Post a Reduce batch that empties the float. → Post a BACS reimburse batch and a payroll reimburse batch.
  - Expected: Float balance drops; at 0 Active Floats shows "Fully Spent" with "Close Float". BACS batch in Out of Pocket "Payment Routing" → "BACS Payments"; payroll batch in "Payroll Additions" (and the payroll run's Add Claims).

- **History of posted batches @p1** — notrun (account-hub/06-petty-cash-posting.spec.js:332)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER → "History". → Filter, search, "Open" a batch.
  - Expected: GET /claims/history-queue; "History — <name> · Browse and edit posted batches."; tiles "Posted this period", "Total posted", Petty Cash, Out of Pocket; the opened batch shows "Save" and "Assign"/"Reassign" but no Post or Escalate (a posted batch stays editable — see defects).

### Cash Recon

- **Start a period @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:390)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "New Reconciliation". → Period (last 12 months), "Opening Safe Balance", currency → "Begin Reconciliation".
  - Expected: Disabled until amount above 0; POST /reconciliations {opening_safe_balance, currency, period_start, period_end, book_balance, denominations}; edit view; GET /reconciliations/compute-book about 500 ms later.

- **Count cash and see the variance @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:405)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Enter note and coin counts. → Add reconciling items: Paid Out, Received, Timing.
  - Expected: Physical total = Σ value × count; Paid Out and Timing subtract and Received adds to the adjusted book; banner "Balanced", "Variance — shortfall" or "Variance — overage".

- **Save, submit for review, sign off @p0** — notrun (account-hub/06-petty-cash-posting.spec.js:447)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER "Save Draft", then "Submit for Review". → SENIOR_PA opens it → "Sign Off Period". → Reopen.
  - Expected: PATCH /reconciliations/:id; POST …/submit-for-review → Under Review; senior POST …/sign-off → back to the list, Signed Off; a signed-off period opens read-only. Non-seniors have no "Sign Off Period"; seniors have no "Submit for Review".

### Settings and team rights

- **Save custodian account and BS range @p0** — notrun (account-hub/06-petty-cash.spec.js:292)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Enter Float Custodian Account, BS Code From, BS Code To; section Save.
  - Expected: PATCH /settings {float_custodian_account, bs_code_from, bs_code_to}; toast; module re-reads GET /metadata. The BS range doesn't restrict any picker.

- **Add and edit a team member @p0** — notrun (account-hub/06-petty-cash.spec.js:320)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "Add Member", pick an accounts user, Posting Limit 100, Can Override on, "Add". → Edit the row, toggle "Is Senior", "Update". → Add another with limit 0.
  - Expected: Picker lists accounts-team users not yet added; saved at once via PATCH /settings {team_members}; row "£100" / "Above £100 Submit to Senior", Can Override "Yes"; Is Senior forces "Unlimited" and Can Override on (both disabled) and shows "Senior"; limit 0 shows "No access" / "Submit to Senior".

- **Department coordinators @p0** — notrun (account-hub/06-petty-cash.spec.js:399)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "Add Coordinator", Save with nothing picked. → Pick a department, then users (disabled until a department; lists that department's users), toggle Coding Required and View Floats, Save.
  - Expected: 1: "Select a department" / "Select at least one user". 2: PATCH /settings {department_coordinators:[{department_id, user_ids, coding_required, view_department_floats}]}; the coordinator's tabs change (AH-CASH-007).

- **Approval & Override toggles @p0** — notrun (account-hub/06-petty-cash.spec.js:473)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Toggle "Accountant Override — Float Requests", "Accountant Override — Receipt Batches", "Require Coordinator Coding", "Senior Sign-off Required"; Save.
  - Expected: PATCH /settings {approval_override}; effects in AH-CASH-030, AH-CASH-080, AH-CASH-060, AH-CASH-006. The sign-off hint says "above £500" but no threshold exists (defect).

### Journey J1 — issue, complete details, sign, send for approval

- **Pick the crew member, role auto-fills @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:139)
  - Where: 07 Deal Memo
  - Steps: Crew Details → Edit. → Choose CREW2 in "— Select Crew Member —".
  - Expected: Department and Designation fill from the member's record; Unit pre-fills with the member's home unit; Crew Type ("Shooting Crew" / "Non-Shooting Crew") and Call Sheet Tier are set.

- **Preview before issuing @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:153)
  - Where: 07 Deal Memo
  - Steps: Fill a valid deal. → "Issue Memo". → "Back to deal". → "Issue Memo" again.
  - Expected: Overlay "Review before issuing" with the crew name; "Back to deal" closes it with no request.

- **Accountant nominal Now / Later @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:178)
  - Where: 07 Deal Memo · account: ACC leave nominal codes empty
  - Steps: As ACC leave nominal codes empty, Issue → preview → "Issue Memo". → Click "Now". → Repeat and click "Later". → As POST issue with codes empty.
  - Expected: Modal "Nominal codes not added" (Cost Report warning). "Now" opens the Nominal Coding section. "Later" issues. POST sees no prompt.

- **Issue sends the deal to the crew @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:198)
  - Where: 07 Deal Memo
  - Steps: Issue a valid deal for CREW2.
  - Expected: PATCH /deals/:id (no status), then POST /deals/:id/submit; success toast; lands on /deals/:id with status "Issued" (Back goes to All Deals). CREW2 gets a bell notification and sees it on My Deal.

- **One live deal per person @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:219)
  - Where: 07 Deal Memo
  - Steps: Open the crew picker.
  - Expected: CREW (issued deal) and anyone with a draft, rejected, approved or active deal is absent; the member with a deactivated deal is listed.

- **Status quick filters @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:243)
  - Where: 07 Deal Memo
  - Steps: Click each pill: All, Drafts, Issued to Crew, Awaiting Approval, Approved, Rejected, Deactivated.
  - Expected: Each shows only matching rows; "Approved" shows both Approved and Active rows; filtering is client-side (no new request).

- **Row opens editor or page @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:275)
  - Where: 07 Deal Memo
  - Steps: Click a draft row. → Click an issued row.
  - Expected: Draft → /edit builder. Issued → /deals/:id.

- **Crew sees their issued deal @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:310)
  - Where: 07 Deal Memo · account: CREW
  - Steps: As CREW open My Deal.
  - Expected: The memo with the crew bar: "Click a document below to sign it. All must be signed before you can send for approval.", chips Deal Memo, Start Form and any additional documents, buttons "Reject" and "Send for Approval", and "Complete your details". No approval chain, no Edit.

- **Signing waits for required fields @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:325)
  - Where: 07 Deal Memo
  - Steps: With required fields missing, look at the Deal Memo chip. → Click it.
  - Expected: Chip disabled with "Complete your details first"; clicking opens "Not ready yet" listing "Details marked required" fields.

- **Always-required fields and either-or pairs @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:342)
  - Where: 07 Deal Memo
  - Steps: On a deal with nothing marked, open the form. → Type an IBAN only. → Save with Emergency contact name empty.
  - Expected: Required: Full Legal Name, Bank Name, Account Holder Name, Account Number or IBAN, Emergency Contact Name, Contact Number or emergency email. The IBAN clears the account-number requirement. Save still works, with "Some required fields are still empty — you can save now and finish later, but they're needed before you can send this for approval."

- **Complete your details @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:371)
  - Where: 07 Deal Memo
  - Steps: "Complete your details". → Walk the steps with "Continue". → "Save & Finish".
  - Expected: /my-deal/complete with "Your Details — {n} Steps" (Crew Details, Emergency Details, Representative Details, Bank Details, plus Loan Out Company for loan-out status). Save → PATCH /deal/crew-details; back on My Deal the memo shows the new values.

- **Send for Approval lists blockers @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:408)
  - Where: 07 Deal Memo
  - Steps: Required fields done, Deal Memo unsigned. → "Send for Approval". → Sign from the chip inside the pop-up.
  - Expected: "Not ready yet" — "Finish these first:" plus "Documents to sign" chips (signed ones ticked), "Got it". The pop-up hides while the signer is open and closes itself once nothing is missing.

- **Sign the deal memo in-app @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:423)
  - Where: 07 Deal Memo
  - Steps: Complete details. → Click "Deal Memo". → Pick or draw a signature in the saved-signature picker. → Drag the stamp, "Sign Document".
  - Expected: Signer "Sign: Deal Memo" with "Drag your signature to where you want to sign…". Uploads the flattened PDF, then POST /deals/:id/deal-pdf/sign; the chip shows signed.

- **Editing a signed deal rewinds it @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:443)
  - Where: 07 Deal Memo · account: POST
  - Steps: CREW signs an issued deal. → As POST open it via "Edit Deal Memo" and change a note. → Wait for autosave; reopen /deals/:id.
  - Expected: Status back to "Issued"; the crew's signed PDF is gone and the deal page shows "Details changed after signing."

- **Send for approval @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:469)
  - Where: 07 Deal Memo
  - Steps: All required documents signed. → "Send for Approval".
  - Expected: POST /deals/:id/send-for-approval; success toast; status "Awaiting Approval"; reference changes from DRFT-… to DM-…; Sign/Send controls gone; AP1 gets a badge on Approval Queue.

### Journey J2 — approve, go live, nominals, notices, deactivate

- **Issue Memo on an awaiting deal keeps its signatures (possible defect)** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:512)
  - Where: 07 Deal Memo
  - Steps: Open an awaiting_approval deal via "Edit Deal Memo". → Click "Issue Memo" (the builder never switches to Save).
  - Expected: PATCH rewinds the deal (signatures/approvals cleared), then /submit runs. Record the status and whether approvals were lost.

- **Approval card and Approve & Sign survive an empty department config (possible defect; metadata stubbed)** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:537)
  - Where: 07 Deal Memo · account: AP1
  - Steps: As AP1 open an awaiting deal. → As CREW open it on My Deal.
  - Expected: AP1: card "Approval Chain" with "Level 1", "Level 2" nodes (current / pending / approved) and "{n}/{total} approved". CREW: no chain card.

- **Approval Queue rows @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:559)
  - Where: 07 Deal Memo · account: AP1
  - Steps: As AP1 open "Approval Queue".
  - Expected: GET /deals/approval. Rows show crew name, reference, "Dept · Role", "Submitted" date, "Day Rate" "{amount}/day", a history icon and "Approve & Sign". Empty: "Nothing waiting on you" + hint.

- **Level 1 approves and signs @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:579)
  - Where: 07 Deal Memo
  - Steps: As AP1 click "Approve & Sign". → Sign the Deal Memo.
  - Expected: POST /deals/:id/deal-pdf/sign, then POST /deals/:id/approve; success toast; Level 1 shows approved; the deal leaves AP1's queue and appears in AP2's.

- **Level 2 cannot jump the queue @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:607)
  - Where: 07 Deal Memo
  - Steps: Before Level 1 approves, open the deal as AP2.
  - Expected: No "Approve & Sign" (not AP2's turn); chain shows Level 1 current.

- **Final level settles the deal @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:611)
  - Where: 07 Deal Memo
  - Steps: As AP2 Approve & Sign.
  - Expected: Approve succeeds; the page refetches; status becomes "Approved" or "Active" (server decides); no Approve button remains.

- **Edit menu by status and role — active union deal @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:631)
  - Where: 07 Deal Memo · account: ACC
  - Steps: As ACC open an active non-union deal, then an active union deal, then a rejected deal. → As POST open the active union deal.
  - Expected: ACC non-union active: menu "Update Non-Union Pay breakdown", "Update Nominals" (no "Edit Deal Memo"). ACC union active: single "Update Nominals". Rejected: only "Edit Deal Memo". POST union active: no Edit control at all.

- **Re-code a live deal @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:640)
  - Where: 07 Deal Memo · account: ACC
  - Steps: As ACC: "Update Nominals". → Change one overtime row's code. → Save.
  - Expected: Modal "Update Nominals — {name}"; PATCH /deals/:id/nominal-codes with only the changed row (row_id + raw_label); status unchanged; signed PDFs intact. "Cancel" with unsaved codes asks "Unsaved changes" with "Save and leave" / "Discard changes".

- **Notices groups and membership @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:693)
  - Where: /film-tools/deal-memo/notices
  - Steps: Open "Notices".
  - Expected: Info banner "All Crew Deal Memos will show up here with end date and notice period, so you can send them notices." Groups "Deactivated", "With Notice Period", "Without Notice Period". Draft, issued, awaiting and approved deals are absent. GET /deals.

- **Notice period label for current tokens (possible defect, P2)** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:717)
  - Where: /film-tools/deal-memo/notices
  - Steps: Look at a 2_week deal's Notice Period column and its filled notice body. → Add a 1_week deal that ends later than a 2_week deal.
  - Expected: Code: label shows "2 Week" (the deal page shows "2 Weeks"), also in the mailed {{notice_period}}. Both tokens rank equal, so the group sorts by end date only, not longest notice first. Record actual.

- **Send one notice (send stubbed — no email) @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:729)
  - Where: /film-tools/deal-memo/notices
  - Steps: In With Notice Period click "Send". → Check the defaults. → Edit the body, "Send".
  - Expected: Modal "Send Notice" — "Send the end-of-contract notice for {name}{role}." with "You can edit this notice below before sending it." "Last pay day" defaults to the contract end date; body filled from the template ({{crew_name}}, {{contract_end_date}}, {{last_pay_day}}, {{notice_period}}). POST /deals/:id/send-notice {last_pay_day, content} (edited text, invisible characters stripped); row shows "Notice Sent by: {name}".

- **Send one notice (real email) @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:767)
  - Where: /film-tools/deal-memo/notices
  - Steps: In With Notice Period click "Send". → Check the defaults. → Edit the body, "Send".
  - Expected: Modal "Send Notice" — "Send the end-of-contract notice for {name}{role}." with "You can edit this notice below before sending it." "Last pay day" defaults to the contract end date; body filled from the template ({{crew_name}}, {{contract_end_date}}, {{last_pay_day}}, {{notice_period}}). POST /deals/:id/send-notice {last_pay_day, content} (edited text, invisible characters stripped); row shows "Notice Sent by: {name}".

- **Deactivate a departed crew member (departure simulated) @p0** — notrun (account-hub/07-deal-memo-lifecycle.spec.js:772)
  - Where: /film-tools/deal-memo/notices
  - Steps: Remove CREW from the project (account status removed/left). → In Deactivated click "Deactivate". → Keep the default date and confirm.
  - Expected: Modal "Deactivate deal memo" ("…Payroll stops paying on/after the last pay date."), "Last Pay Date" defaults to the contract end date (today if none). POST /deals/:id/deactivate {last_pay_date} (UTC midnight epoch); the button disappears.

### NDAs — register

- **Posting user register @p0 @smoke** — notrun (account-hub/07-deal-memo-nda.spec.js:24)
  - Where: 07 Deal Memo · account: POST
  - Steps: As POST open "NDAs".
  - Expected: Banner "Every crew member on the project shows up here with their NDA status, so you can send, chase or replace." One row per member (pending members included) plus external recipients. Buttons "NDA Templates", "My NDA". Filters All / Not Sent / Not Signed / Awaiting Countersign / Signed. GET /api/v2/deal-memo/nda.

### NDA journey — send, recipient signs, approver countersigns

- **Send an NDA to a member @p0** — notrun (account-hub/07-deal-memo-nda.spec.js:72)
  - Where: 07 Deal Memo
  - Steps: Row "Send" for CREW. → Check the page. → "Send NDA".
  - Expected: /nda/send with "Send NDA · {name}"; the first template applied; "Document name", "Disclosing party". Upload of the HTML, then POST /api/v2/deal-memo/nda/send/template; toast "NDA sent." (or server text); lands on /nda/:id; register row "Not Signed".

- **Recipient signs @p0** — notrun (account-hub/07-deal-memo-nda.spec.js:107)
  - Where: 07 Deal Memo
  - Steps: Leave one field empty, "Sign NDA". → "Sign anyway". → Sign in the signer.
  - Expected: Warning "Sign with details still empty?" — "{fields} — still empty. They will print as \"-\" in the signed NDA." Then POST /nda/:id/sign; status "Awaiting Countersign"; "Download" available.

- **Approver countersigns @p0** — notrun (account-hub/07-deal-memo-nda.spec.js:123)
  - Where: 07 Deal Memo
  - Steps: As AP1 click "Sign" on the register row. → Sign. → Repeat as AP2.
  - Expected: /nda/:id opens with the signer already open; POST /nda/:id/sign-approver; toast "NDA signed.". After every level has signed the status is "Signed" (completed).

### Journey N1 — a non-union deal, issued, signed and sent

- **setup: issue a non-union deal to the crew account, crew signs and sends @p0** — notrun (account-hub/07-deal-memo-nonunion.spec.js:106)
  - Where: 07 Deal Memo · account: POST
  - Steps: As POST on an active non-union deal: "Update Non-Union Pay breakdown". → Change an overtime row and save the grid.
  - Expected: Full-page grid with eyebrow "Deal Memos · {ref}"; PATCH /deals/:id/deal-rules; success toast; status stays "Active"; the crew sees the amendment banner.

### Journey N2 — live non-union deal: amend pay rules, crew acknowledges

- **setup: approve the non-union deal live @p0** — notrun (account-hub/07-deal-memo-nonunion.spec.js:133)
  - Where: 07 Deal Memo · account: POST
  - Steps: As POST on an active non-union deal: "Update Non-Union Pay breakdown". → Change an overtime row and save the grid.
  - Expected: Full-page grid with eyebrow "Deal Memos · {ref}"; PATCH /deals/:id/deal-rules; success toast; status stays "Active"; the crew sees the amendment banner.

- **Edit menu by status and role — active non-union deal @p0** — notrun (account-hub/07-deal-memo-nonunion.spec.js:143)
  - Where: 07 Deal Memo · account: ACC
  - Steps: As ACC open an active non-union deal, then an active union deal, then a rejected deal. → As POST open the active union deal.
  - Expected: ACC non-union active: menu "Update Non-Union Pay breakdown", "Update Nominals" (no "Edit Deal Memo"). ACC union active: single "Update Nominals". Rejected: only "Edit Deal Memo". POST union active: no Edit control at all.

- **Amend non-union pay rules on a live deal @p0** — notrun (account-hub/07-deal-memo-nonunion.spec.js:149)
  - Where: 07 Deal Memo · account: POST
  - Steps: As POST on an active non-union deal: "Update Non-Union Pay breakdown". → Change an overtime row and save the grid.
  - Expected: Full-page grid with eyebrow "Deal Memos · {ref}"; PATCH /deals/:id/deal-rules; success toast; status stays "Active"; the crew sees the amendment banner.

- **Crew acknowledges the amendment @p0** — notrun (account-hub/07-deal-memo-nonunion.spec.js:179)
  - Where: 07 Deal Memo · account: CREW
  - Steps: As CREW open My Deal after AH-DM-145. → "Acknowledge". → As POST and AP1 view the deal.
  - Expected: Banner "Your deal memo has been amended" — "Please look at the amended working hours and conditions…". POST /deal/acknowledge-amendment; banner gone. POST and AP1 never see the banner.

### Access & navigation

- **Approval Queue tile only for approvers @p0 @smoke** — notrun (account-hub/08-timecard-access.spec.js:125)
  - Where: /film-tools/timecard
  - Steps: Open the landing as Approver T1. → Open it as Crew A (not an approver).
  - Expected: T1 sees "Approval Queue" / "Awaiting sign-off"; Crew A does not.

### Access & navigation › as Crew A (crew account)

- **Landing loads for a crew member @p0 @smoke** — notrun (account-hub/08-timecard-access.spec.js:63)
  - Where: /film-tools/timecard · account: Crew A
  - Steps: Log in as Crew A. → Open /film-tools/timecard.
  - Expected: Header eyebrow and title "Time Card"; Daily Login panel on the left; tile "Weekly Time Card" with sub "Current week" on the right; GET /api/v2/payroll/timecards/metadata fires once.

- **Approval Queue guards itself @p0 @smoke** — notrun (account-hub/08-timecard-access.spec.js:89)
  - Where: /film-tools/timecard · account: Crew A
  - Steps: As Crew A (not approver) open /film-tools/timecard/approval-queue.
  - Expected: After metadata loads, redirected to /film-tools/timecard.

### Access & navigation › as User D (crew account, timecard_tool view off by stub)

- **User without tool access is ejected @p0 @smoke** — notrun (account-hub/08-timecard-access.spec.js:111)
  - Where: /film-tools/timecard · account: User D
  - Steps: Log in as User D. → Open /film-tools/timecard/weekly-timecard.
  - Expected: Redirected to /film-tools (replace); no timecard calls.

### Daily login

- **Today in the payable window, nothing recorded @p0** — notrun (account-hub/08-timecard-access.spec.js:176)
  - Where: /film-tools/timecard · account: Crew A
  - Steps: As Crew A open the landing.
  - Expected: Date eyebrow in the browser's local date (e.g. "MONDAY, 16 NOVEMBER 2026"); title "Log in or log out", sub "Click below to record it for your time card"; buttons "Log In" and "Log Out", each with a "Show time" switch; GET /daily-login?date_from&date_to&timezone=UTC spans the 7 pay-period days.

- **Log In, with and without "Show time" @p0** — notrun (account-hub/08-timecard-access.spec.js:204)
  - Where: /film-tools/timecard
  - Steps: Grant geolocation. → Click "Log In". → On another day (or after voiding the record) turn on the first "Show time" and click "Log In".
  - Expected: POST /daily-login/<todayUTC>/login with is_manual:false, timezone:"UTC", lat, long, is_gps:true. Show time off: modal "Login recorded" / "Saved with your current location. You can see and edit this in your weekly time card." / "Got it"; title "Logged in", sub "Login recorded"; the button becomes a grey "Login recorded" chip. Show time on: no modal; sub "Since HH:MM"; chip "Logged in" plus the time.

- **Log Out completes the day @p0** — notrun (account-hub/08-timecard-access.spec.js:266)
  - Where: /film-tools/timecard
  - Steps: After logging in, click "Log Out".
  - Expected: POST …/logout; title "Day complete", sub "Login and logout recorded". Logging out without a login shows "Logged out" / "Logout recorded".

### Submit, approve, reject, resubmit (two users)

- **Crew submits the week @p0** — notrun (account-hub/08-timecard-approval.spec.js:73)
  - Where: /film-tools/timecard
  - Steps: Fill the week, click "Submit for approval".
  - Expected: POST /weekly/:id/submit with the full save payload (no separate PATCH first); server toast; pill "AWAITING APPROVAL"; "Submit for approval" and "Assign" hide; "Save" stays.

- **Approver sees the submitted card @p0** — notrun (account-hub/08-timecard-approval.spec.js:95)
  - Where: /film-tools/timecard · account: T1
  - Steps: As T1 open "Approval Queue". → Click Crew A's row.
  - Expected: GET /weekly/approval; header pill "Awaiting you" with count; row with name, role, department, week, "L1"; click → GET /weekly/:id; detail shows Days, Hours, Basic, Overtime, "Allowance / Rentals / Extras", Total, "Daily breakdown" and "Approve <amount>" with "Approving sends this timecard to payroll."

- **Reject one card with a reason @p0** — notrun (account-hub/08-timecard-approval.spec.js:126)
  - Where: /film-tools/timecard · account: T1 select the card
  - Steps: As T1 select the card, click "Reject". → Try "Confirm Rejection" empty, then type a reason.
  - Expected: Modal "Reject time card" / "Tell the crew member what to fix…" / "Rejection Reason"; "Confirm Rejection" disabled until a non-blank reason; POST /weekly/:id/reject {reason}; badge read; list reloads.

- **Crew sees the rejection @p0** — notrun (account-hub/08-timecard-approval.spec.js:156)
  - Where: /film-tools/timecard · account: Crew A
  - Steps: As Crew A open the rejected week.
  - Expected: Pill "REJECTED"; right panel "Rejection" card with approver name, time, "Reason" and the text ("No reason provided." if blank); "Submit for approval" and "Assign" show again.

- **Edit and resubmit @p0** — notrun (account-hub/08-timecard-approval.spec.js:171)
  - Where: /film-tools/timecard
  - Steps: Fix a day, click "Submit for approval".
  - Expected: POST /weekly/:id/submit; pill "AWAITING APPROVAL"; rejection card gone; card back at tier 1.

- **Non-final tier approves @p0** — notrun (account-hub/08-timecard-approval.spec.js:188)
  - Where: /film-tools/timecard · account: Crew A reload Weekly
  - Steps: As T1 click "Approve <amount>". → As Crew A reload Weekly.
  - Expected: POST /weekly/:id/approve; toast; notification:read for approval_queue_label after the call; queue reloads. Crew still sees "AWAITING APPROVAL" and T1 under "Approvals" as "Level 1"; card now in T2's queue.

- **Editing mid-approval asks before resetting @p0** — notrun (account-hub/08-timecard-approval.spec.js:195)
  - Where: /film-tools/timecard
  - Steps: After T1 approved, as Crew A edit a day and click "Save". → Click "Continue & reset approvals". → Repeat with "Replace invoice".
  - Expected: Dialog "Reset approval cycle?" / "This timecard is under the approval process. Editing it will reset the approvals and every tier will need to re-review against the updated values."; confirm → PATCH; "Approvals" list clears; card back at T1. Cancel sends nothing. Invoice attach / replace / remove use the same dialog.

- **Last tier approves @p0** — notrun (account-hub/08-timecard-approval.spec.js:241)
  - Where: /film-tools/timecard · account: T2 approve
  - Steps: As T2 approve. → As Crew A reload.
  - Expected: Pill "APPROVED"; Submit and Assign hidden; "Save" still visible; days still editable.

### Approval Queue (stubbed states)

- **Card listed as `submitted` @p1 @smoke** — notrun (account-hub/08-timecard-approval.spec.js:310)
  - Where: /film-tools/timecard
  - Steps: Stub GET /weekly/approval with a card status:"submitted"; select it.
  - Expected: Expected: Approve / Reject available. Code renders the footer only for pending (mapped from awaiting_approval), so no single-card actions show.

- **Batch with cards the approver can't act on @p2 @smoke** — notrun (account-hub/08-timecard-approval.spec.js:323)
  - Where: /film-tools/timecard
  - Steps: Stub POST /weekly/batch/approve → 200 {approved:[a], skipped:[{id:b, reason}]}.
  - Expected: Expected: user told b was skipped. Code shows only a success toast and b reappears after reload.

### Configuration

- **Production-controlled, with and without a controller @p0** — notrun (account-hub/08-timecard-config.spec.js:76)
  - Where: /film-tools/timecard
  - Steps: Click "Production-Controlled". → Pick a user in "Production Controller" (Production / Producers users only), click "Save & Apply". → On a config with no controller stored, pick "Production-Controlled" and save without picking one.
  - Expected: Top bar "Unsaved changes" → "All changes saved"; PUT /config {overall_control_model:"production", hybrid_default:null, approval_cadence, production_controller:<id>}; server toast. Step 3 saves production_controller:null with no warning (see Open questions).

- **Department Setup save @p0** — notrun (account-hub/08-timecard-config.spec.js:118)
  - Where: /film-tools/timecard
  - Steps: Click "Open Department Setup". → Set one department "Department-Controlled", click "Assign", pick a user, set another "Production-Controlled". → Click "Save changes".
  - Expected: "Assign completer — <dept>" lists only that department's users; production rows read "Set Production Controller above" until one is picked; "Save changes" disabled until dirty; PUT /department-completers {rows} with every department, then PUT /config with the controller; toast; "<n> / <m> completers assigned" updates.

### Times, dates, breaks, overnight shifts

- **Enter My Call manually @p0** — notrun (account-hub/08-timecard-weekly.spec.js:351)
  - Where: /film-tools/timecard
  - Steps: On a fillable SWD day with no GPS login, click My Call. → Pick 08:00, type a reason, click "Save Amendment". → Reload.
  - Expected: Modal "Amend Crew In Time — <day> <date>" with "New Crew In Time", "Original (auto-filled)", "Reason (optional)"; "Save Amendment" disabled until a time is picked; PATCH /daily-login/<dateMs> with both login_details (is_manual:true) and logout_details, timezone:"UTC"; amendment line "Amended …: — → 08:00. “reason”". After reload the line is gone (the reason is session-only); the day's "History" link ("Daily Login History", GET /daily-login/<dateMs>/history) shows the change.

- **Overnight shift @p0** — notrun (account-hub/08-timecard-weekly.spec.js:411)
  - Where: /film-tools/timecard
  - Steps: My Call 18:00, My Wrap 04:00, no meal (deal SWD 10 h).
  - Expected: Worked = 10 h (wrap rolled to next day); no OT; saved minutes_worked: 600.

- **Worked times are UTC wall-clock in any browser zone @p0** — notrun (account-hub/08-timecard-weekly.spec.js:434)
  - Where: /film-tools/timecard
  - Steps: With timezoneId:"America/Los_Angeles" set My Call 07:30 and save. → Reopen with timezoneId:"Asia/Kolkata".
  - Expected: PATCH login_details.time = day 00:00Z + 07:30; both browsers show 07:30 (no shift).

### Production-report fill

- **Auto-seed from the wrap report @p0** — notrun (account-hub/08-timecard-weekly.spec.js:484)
  - Where: /film-tools/timecard
  - Steps: Open a week where a past day has a wrap report and the card's fields are empty.
  - Expected: GET /api/v2/ad-shoot-days/day-details?week_start=<ms>; empty Unit Call, Unit Wrap, meals (meal_N_in → Meal N Start, meal_N_out → Meal N End) and day type fill; an auto-save PATCH follows without user action.

- **Seed never overwrites @p0** — notrun (account-hub/08-timecard-weekly.spec.js:520)
  - Where: /film-tools/timecard
  - Steps: Save Unit Call 07:00 on a day whose report says 06:30. → Leave and reopen the week.
  - Expected: Unit Call stays 07:00; seed fills only blank fields.

- **"Fill from Production Report" overwrites @p0** — notrun (account-hub/08-timecard-weekly.spec.js:550)
  - Where: /film-tools/timecard
  - Steps: With different values already in the row, click "Fill from Production Report".
  - Expected: Unit Call, Unit Wrap, My Call (Time In), My Wrap (Time Out) and meals overwritten; panel closes; values saved on the next auto-save.

### Overtime and pay calculation

- **Engine bundle loads for the deal's agreement @p0** — notrun (account-hub/08-timecard-weekly.spec.js:610)
  - Where: /film-tools/timecard
  - Steps: Open Weekly.
  - Expected: GET /api/v2/payroll/ot-engine/manifest then GET /api/v2/payroll/ot-engine/<bundle> for deal.agreement_identifier (falls back to default when absent from the manifest); bundle runs from a blob: script.

- **Basic + exclusive holiday pay @p0** — notrun (account-hub/08-timecard-weekly.spec.js:649)
  - Where: /film-tools/timecard
  - Steps: Deal: day rate 400, hp_rate 0.1207, not included. → Fill one SWD day 08:00–18:00 (10 h basic).
  - Expected: Day Total 400.00; Weekly Pay Estimate "Basic" 400.00 and "Holiday pay (Exclusive)" 48.28 marked "accrual · not in Weekly Pay Estimate"; estimate total excludes the 48.28.

- **Basic with holiday pay included @p0** — notrun (account-hub/08-timecard-weekly.spec.js:670)
  - Where: /film-tools/timecard
  - Steps: Deal: day rate 400, hp_rate 0.1207, included. → Same day.
  - Expected: Basic 356.92 (400 / 1.1207), "Holiday pay (Inclusive)" 43.08; Day Total 356.92.

- **Daily OT past basic hours, with increments @p0** — notrun (account-hub/08-timecard-weekly.spec.js:688)
  - Where: /film-tools/timecard
  - Steps: Deal SWD 10 h, OT row with no increment. → My Call 08:00, My Wrap 20:00, Meal 13:00–14:00, no Unit Call / Wrap. → Set the OT row's increment to 30 and My Wrap to 19:50.
  - Expected: Step 2: worked 11 h (12 h − 1 h meal); one OT line of 60 min in the expanded day and "OT Breakdown"; "OTs" KPI counts it; Day Total = basic + OT pay. Step 3: 50 min excess is billed as 60 min.

- **Broken turnaround, inside the week and across it @p0** — notrun (account-hub/08-timecard-weekly.spec.js:730)
  - Where: /film-tools/timecard
  - Steps: Deal turnaround row "less than 11 h". → Monday My Wrap 23:00; Tuesday My Call 07:00. → Previous week's last day My Wrap 23:30 (saved); this week's day 1 My Call 08:00.
  - Expected: Tuesday: turnaround line of 3 h ("… below 11h …"); "Turnarounds" / "BTA flags" KPI counts it. Day 1: 2 h 30 line from tc.prior_week.priorTurnaroundDay; none when the prior day has no logout.

- **Save persists the priced lines @p0** — notrun (account-hub/08-timecard-weekly.spec.js:773)
  - Where: /film-tools/timecard
  - Steps: Fill a week and click "Save".
  - Expected: PATCH days[].rates_ots has a "Basic Day Rate" line with nominal_code = deal.rates.nominal_code, plus each engine line; minutes_worked per day; total_pay = Weekly Pay Estimate; company_id sent only when the deal has one.

### Allowances

- **5 Days Week rental pro-rates by worked days @p0** — notrun (account-hub/08-timecard-weekly.spec.js:817)
  - Where: /film-tools/timecard
  - Steps: Weekly rental 350.00 basis "5 Days Week". → Week with 3 worked days, rest Day Off. → Change a Day Off to SWD.
  - Expected: Row 210.00 with chip "3 of 5 days" → 280.00 after the change; weekly_allowances row qty 0.6 → 0.8.

### Autosave & drafts

- **Auto-save after an edit @p0** — notrun (account-hub/08-timecard-weekly.spec.js:851)
  - Where: /film-tools/timecard
  - Steps: Change a day type and a meal quickly. → Edit again and switch tab within 2 s.
  - Expected: Pill "Saving…" then "Auto Saved"; one PATCH /weekly/:id about 2 s after the last edit (whole-week payload). Hiding the tab flushes at once.

- **Manual Save @p0** — notrun (account-hub/08-timecard-weekly.spec.js:893)
  - Where: /film-tools/timecard
  - Steps: Edit, click "Save".
  - Expected: Button shows "Saving…"; PATCH /weekly/:id; server success toast; no duplicate auto-save PATCH of the same bytes.

### Status-driven UI (stub)

- **Submit button by status @p0 @smoke** — notrun (account-hub/08-timecard-weekly.spec.js:927)
  - Where: /film-tools/timecard
  - Steps: Stub GET /weekly with each status.
  - Expected: "Submit for approval" shows for draft, rejected, queried; hidden for awaiting_approval, submitted, pending, approved, final_approved, locked, paid, unpaid, posted. A queried week shows pill "QUERIED", stays editable and has no rejection card (no web action sets queried).

- **Payroll-locked week is read-only @p0 @smoke** — notrun (account-hub/08-timecard-weekly.spec.js:961)
  - Where: /film-tools/timecard
  - Steps: Open a week in final_approved, locked, paid, unpaid, posted (real or stubbed).
  - Expected: Pills "ACCT APPROVED", "LOCKED", "PAID", "UNPAID", "POSTED"; no "Save", "Submit for approval", "Assign", auto-save pill; day types as plain chips; no amend, meal, allowance add/remove, "Prod Report" pill or Fill; "Attach Invoice" disabled.

### Claims & deductions

- **Attach and detach a cash-expense batch @p0** — notrun (account-hub/09-payroll-claims.spec.js:48)
  - Where: /film-tools/account-hub/payroll
  - Steps: Under "Available to add" click "+ Add". → Click "Remove" on it.
  - Expected: Available batches carry a "CASH" (petty cash) or "OOP" badge. POST …/weekly/:id/attach-claim with cash_expense_batch_id; the batch moves to "On this payroll" with "Cash expense · batch {id}"; "Payroll running total" grows. Remove sends POST …/detach-claim; totals return.

- **Manual claim line @p0** — notrun (account-hub/09-payroll-claims.spec.js:85)
  - Where: /film-tools/account-hub/payroll
  - Steps: "Add a manual line". → Description "Kit delivery", Amount 25, Nominal code 5300. → "Add line". → Remove it.
  - Expected: POST attach-claim with claim_name, claim_amount: 25, claim_currency (the timecard currency), nominal_code; line listed under "On this payroll" with "Manual line"; form closes. Removal via detach-claim.

- **Add and remove a deduction @p0** — notrun (account-hub/09-payroll-claims.spec.js:124)
  - Where: /film-tools/account-hub/payroll
  - Steps: Drawer → "Add Deduction". → Description "Advance recovery", Amount 50 → "Add deduction". → Remove it.
  - Expected: Title "Deductions · {name}"; POST …/add-deduction with rate_type: "flat", rate_amount: 50; row under "On this timecard" with "Flat amount"; "Total deductions: £50.00"; remove sends POST …/remove-deduction.

### Journal ledger

- **Code and save @p0** — notrun (account-hub/09-payroll-ledger.spec.js:93)
  - Where: /film-tools/account-hub/payroll
  - Steps: Set a nominal code on a pay line. → Click "Save".
  - Expected: "1 unsaved" appears; POST /api/v2/payroll/runs/journal-ledger { action: "save", week_starting, posting_week (same value), timecards, run_lines }; pay lines carry debit: null; toast; refetch; unsaved count clears.

- **Post gate: fixable cards @p0** — notrun (account-hub/09-payroll-ledger.spec.js:131)
  - Where: /film-tools/account-hub/payroll · account: A-PAFC
  - Steps: As A-PAFC, week with approved and locked cards that have rows. → Click "Post". → Confirm "Final Approve & Lock N & Post".
  - Expected: Modal "Accountant-approve & lock N timecards?"; after the action and refetch the gates run again and the post modal opens. A-FA gets "Final Approve N & Post"; A-PLAIN gets the alert in AH-PAY-103.

- **Post the journal @p0** — notrun (account-hub/09-payroll-ledger.spec.js:161)
  - Where: /film-tools/account-hub/payroll
  - Steps: Complete all lines on a week with paid/locked cards. → "Post" → check counts. → Click "Post N".
  - Expected: Modal "Post to Ledger — {week}", "N timecards · M ledger lines · £gross", no bank field; POST /runs/journal-ledger { action: "post", effective_date, … }; toast "Posted as {journal}"; cards become "Posted".

### Processing (pull time cards, edit lines)

- **Crew drawer from a current-week row @p0** — notrun (account-hub/09-payroll-processing.spec.js:132)
  - Where: /film-tools/account-hub/payroll
  - Steps: "Weekly Total" → click a row.
  - Expected: Drawer opens; GET /api/v2/payroll/timecards/weekly/:id; header buttons "Add Claims" and "Add Deduction"; footer "View Time Card" opens the read-only week ("{name} — W/E {range}").

- **Outstanding detail modal @p0** — notrun (account-hub/09-payroll-processing.spec.js:151)
  - Where: /film-tools/account-hub/payroll
  - Steps: "Outstanding" → click a row. → Click "View Time Card" on a week card. → Stub GET /api/v2/payroll/outstanding/:userId → 500 / empty.
  - Expected: Modal "Outstanding · {name}", hint "Every week with a timecard, newest first…", one card per week with "+ Add Claims", "+ Add Deduction", footer "Total: {amount}". Step 2 opens "{name} — W/E {date}". Step 3: "Couldn't load outstanding weeks for this crew member." / "No outstanding weeks for this crew member."

- **Send to Payroll Run @p0** — notrun (account-hub/09-payroll-processing.spec.js:191)
  - Where: /film-tools/account-hub/payroll
  - Steps: In the modal tick a draft or awaiting_approval week. → Click "Send 1 to Payroll Run". → Type a reason, click "Send to Run".
  - Expected: Confirm titled "Send to Payroll Run" listing the name and "W/E {date}"; POST /api/v2/payroll/timecards/weekly/:id/send-to-payroll-run (reason only when typed); success toast (server message, else "1 timecard sent to the Payroll Run."); modal weeks refetch, the week reads "Approved"; Outstanding list reloads.

- **Override Approval from the drawer @p0** — notrun (account-hub/09-payroll-processing.spec.js:221)
  - Where: /film-tools/account-hub/payroll · account: A-PLAIN
  - Steps: As A-PLAIN, open an awaiting_approval week's drawer. → Click "Override Approval". → Click "Cancel", reopen, type a reason, click "Override Approval". → Stub the POST → 500 once.
  - Expected: Modal "Override approval" with description and "Reason (optional)"; Cancel keeps the drawer open; POST /api/v2/payroll/timecards/weekly/:id/override; toast from server; status "Approved". On failure: error toast, modal stays open with the typed reason.

- **Processing Mark Paid / Mark Unpaid @p0** — notrun (account-hub/09-payroll-processing.spec.js:247)
  - Where: /film-tools/account-hub/payroll · account: A-FA
  - Steps: As A-FA, "Weekly Total" → locked row "Mark Paid". → On the same row click "Mark Unpaid".
  - Expected: Button shows "Marking…"; POST …/weekly/:id/mark-paid then …/mark-unpaid; server toasts; row status "Paid" then "Unpaid" without a full reload. Card ends unpaid, not locked.

### Claims & deductions

- **Unlock from the lock refusal @p0** — notrun (account-hub/09-payroll-processing.spec.js:274)
  - Where: /film-tools/account-hub/payroll · account: A-PA
  - Steps: As A-PA, in Processing open a locked week → "Add Claims". → Trigger a write the server refuses as locked (for example attach a batch). → Click "Unlock this week".
  - Expected: Inline error with "Unlock this week"; POST …/weekly/:id/unlock; the banner disappears (drawer timecard refetched) and the week reads "ACCT Approved". A-FA does not get the button.

### Run & approval

- **Final approver only: Final Approve @p0** — notrun (account-hub/09-payroll-run.spec.js:108)
  - Where: /film-tools/account-hub/payroll · account: A-FA
  - Steps: As A-FA, click "Final Approve" on an approved row.
  - Expected: Button "Approving…"; POST /api/v2/payroll/timecards/weekly/batch/final-approve with [id]; status "ACCT Approved"; row action becomes "View" (no Lock).

- **PA/FC: Final Approve & Lock @p0** — notrun (account-hub/09-payroll-run.spec.js:112)
  - Where: /film-tools/account-hub/payroll · account: A-PAFC
  - Steps: As A-PAFC, click "Final Approve & Lock" on an approved row.
  - Expected: "Working…"; final-approve then lock requests; status "Locked".

- **Locked week opens read-only @p0** — notrun (account-hub/09-payroll-run.spec.js:132)
  - Where: /film-tools/account-hub/payroll
  - Steps: Run drawer of a locked week. → Click "Show Claims", then "Show Deductions".
  - Expected: Header buttons read "Show Claims" / "Show Deductions"; banner "This timecard is locked. To add or remove claims, unlock the timecard first." (deductions twin); no manual-line or deduction form; no Remove buttons. Same for paid, posted, rejected.

- **Unlock from the drawer @p0** — notrun (account-hub/09-payroll-run.spec.js:163)
  - Where: /film-tools/account-hub/payroll · account: A-PA
  - Steps: As A-PA open a locked drawer → "Unlock".
  - Expected: "Unlocking…"; POST …/weekly/:id/unlock; status "ACCT Approved"; drawer stays open so a claim can be added.

- **Payroll accountant only: Lock @p0** — notrun (account-hub/09-payroll-run.spec.js:190)
  - Where: /film-tools/account-hub/payroll · account: A-PA
  - Steps: As A-PA, click "Lock" on a final_approved row.
  - Expected: "Locking…"; POST …/weekly/batch/lock [id]; status "Locked"; action becomes "Mark Paid". An approved row shows "View" for A-PA.

- **Row Mark Paid on a locked week @p0** — notrun (account-hub/09-payroll-run.spec.js:211)
  - Where: /film-tools/account-hub/payroll
  - Steps: Any accountant clicks "Mark Paid" on a locked row.
  - Expected: "Marking…"; POST …/weekly/:id/mark-paid; status "Paid".

- **Mark Unpaid and pay again @p0** — notrun (account-hub/09-payroll-run.spec.js:226)
  - Where: /film-tools/account-hub/payroll
  - Steps: As A-FA click "Mark Unpaid" on a paid row. → Click "Mark Paid" on it.
  - Expected: POST …/mark-unpaid → "Unpaid"; then …/mark-paid → "Paid". A-PLAIN sees "View" on paid and unpaid rows.

- **Toolbar bulk approval by role @p0** — notrun (account-hub/09-payroll-run.spec.js:247)
  - Where: /film-tools/account-hub/payroll
  - Steps: As A-FA select 2 approved rows → "Final Approve · 2". → As A-PAFC select 2 approved + 1 final_approved → "Final Approve & Lock · 2". → As A-PA select 1 final_approved → "Lock · 1". → Confirm each.
  - Expected: Confirm titles "Accountant-approve 2 timecards?", "Accountant-approve & lock 2 timecards?", "Lock 1 timecard?"; buttons "Final Approve 2", "Final Approve & Lock 2", "Lock 1". In step 2 the lock call includes the newly approved ids plus the already final_approved row. Selection clears after refetch.

- **Bulk Mark Paid @p0** — notrun (account-hub/09-payroll-run.spec.js:281)
  - Where: /film-tools/account-hub/payroll
  - Steps: Select locked rows only. → Add an unpaid row. → Confirm.
  - Expected: Label "Mark Locked → Paid · N", then "Mark Paid · N+1"; modal "Mark N timecards as paid?"; POST …/weekly/batch/mark-paid with the locked + unpaid ids; rows "Paid"; attached cash claims flip to paid server-side.

### Today's List

- **List and KPI cards @p0** — notrun (account-hub/11-ad-dashboard.spec.js:403)
  - Where: /film-tools/ad-dashboard
  - Steps: Book today: Direct PAYE A (present), Agency B (unmarked), C (blocked). → Open Today's List.
  - Expected: Cards "On List Today" 3 with "Direct {n} · Agency {n}", "Confirmed Present" 1 with progress bar, "Pending" ("Not yet marked", counts status: pending), "Issues" 1 ("RTW · Chaperone"), "Est. Day Cost" ("Direct PAYE only", or "Direct PAYE · converted to {code}" for mixed currencies). Columns Artiste, Category, Engagement, Arrival, Wrap, Status, ID, Est. Gross. Only rows whose shoot_date is today's UTC midnight.

- **Mark present stamps now; a failed PATCH reverts @p0** — notrun (account-hub/11-ad-dashboard.spec.js:440)
  - Where: /film-tools/ad-dashboard
  - Steps: Freeze clock at local 08:05. → Click "Mark present". → Repeat with PATCH routed to 500.
  - Expected: "Marking…", then arrival "08:05" and toast "{name} marked present"; PATCH /api/v2/supporting-artist-days/:id {attendance_status:'present', call_time: Date.UTC(today,08:05)}; one list GET follows (~0.5–0.9 s). On 500 the cell reverts to "Mark present" and the server message is toasted.

- **Mark out stamps now @p0** — notrun (account-hub/11-ad-dashboard.spec.js:486)
  - Where: /film-tools/ad-dashboard
  - Steps: On a present row click "Mark out".
  - Expected: Wrap shows HH:mm as static text (no editing); toast "{name} wrapped at {HH:mm}"; PATCH {wrap_time} anchored on today.

- **Day voucher modal @p0** — notrun (account-hub/11-ad-dashboard.spec.js:513)
  - Where: /film-tools/ad-dashboard
  - Steps: Wrap a Direct PAYE artiste. → Click "Voucher"; use the side chevrons.
  - Expected: "Voucher" looks disabled until a wrap exists and is absent on Agency, blocked and draft rows. Modal "Voucher — {name}", "{n} of {total}" across the Direct PAYE wrapped queue (aria "Previous voucher"/"Next voucher"); GET /api/v2/supporting-artist-days/:id; shows Call time, Wrap time, Pay breakdown (rates, allowances, additional fees), "Gross Total", "Holiday pay (12.07% accrued)", Meals ("HH:MM–HH:MM" or "None logged"), code VCH-<ref>-<yyyymmdd>. Signed rows read "✓ Signed".

- **A submitted day locks the list (no flash while the header loads) @p0** — notrun (account-hub/11-ad-dashboard.spec.js:588)
  - Where: /film-tools/ad-dashboard
  - Steps: Submit today. → Reopen Today's List.
  - Expected: No "Add Direct"/"Import List" header buttons, no "Add Extra"; unmarked rows show "—" instead of "Mark present"; present unwrapped rows show "—" in Wrap. Buttons don't flash in while the day header loads.

### Add Extras and quick add

- **Book an existing artiste @p0** — notrun (account-hub/11-ad-dashboard.spec.js:630)
  - Where: /film-tools/ad-dashboard
  - Steps: "Existing artiste" → pick one. → Call Time: type 730, blur. → Click "Add to Today’s List".
  - Expected: Picker excludes artistes already on that day; name/email/category/engagement lock; Call Time shows "07:30" (invalid text such as 25:99 reverts, no AM/PM). POST /api/v2/supporting-artist-days/bulk {artiste_ids:[id], call_time: Date.UTC(day,07:30), shoot_date}; toast "Added to today’s list"; form resets; row appears.

- **Create a new external artiste on the day @p0** — notrun (account-hub/11-ad-dashboard.spec.js:675)
  - Where: /film-tools/ad-dashboard
  - Steps: Leave Existing empty. → Try: blank email; ana@test; blank Last name. → Fill First "Ana", Last "Diaz", Email "ana@test.dev", Engagement "Agency", pick an agency under "Agency (optional)". → Click "Create & Add to Today".
  - Expected: Button disabled in every step-2 case. Agency picker lists GET /api/v2/ad-rate-config/vendors ("No agencies registered" when empty) and is hidden for Direct PAYE. POST /api/v2/artistes/with-day {artiste:{is_external:true, personal_details:{first_name,last_name,email}, category:'general_sa', engagement_type:'agency', agency_id, agency_name}, call_time:null, shoot_date}; toast "Ana Diaz created & added to today".

- **A submitted target day takes no more extras @p0** — notrun (account-hub/11-ad-dashboard.spec.js:737)
  - Where: /film-tools/ad-dashboard
  - Steps: Submit today. → Add Extras → Today, then Tomorrow.
  - Expected: Today: no add panel (list only). Tomorrow: form available. Any quick-add host on a submitted day shows "Today’s day is submitted — read only. No further extras can be added to it. Add them to a later shoot day instead."

### Attendance

- **Present at a specific time @p0** — notrun (account-hub/11-ad-dashboard.spec.js:775)
  - Where: /film-tools/ad-dashboard
  - Steps: Open Attendance ("Mark arrivals"). → Click the chevron "Enter a specific time", type 07:15, "Save".
  - Expected: PATCH {attendance_status:'present', call_time: Date.UTC(today,07:15)}; toast "{name} present · 07:15"; row shows "Arrived" time chip 07:15 and a green check. "Mark present" without a time toasts "{name} present".

### Access & navigation

- **The tile opens the portal with no code step @p0 @smoke** — notrun (account-hub/11-supporting-artists.spec.js:71)
  - Where: /film-tools/supporting-artists · account: internal artiste
  - Steps: Sign in as an internal artiste. → Click the Supporting Artistes tile. → Click the back chevron.
  - Expected: URL /film-tools/supporting-artists/dashboard; no OTP screen; sidebar titled "Extras Portal" with groups "Overview" (Dashboard), "Work" (My Vouchers, Productions, Pay History), "My Account" (Queries, Profile). Back chevron (aria "Back to Film Tools") → /film-tools.

### Access & navigation › crew with the SA view stubbed off

- **No view right → Film Tools @p0 @smoke** — notrun (account-hub/11-supporting-artists.spec.js:94)
  - Where: /film-tools/supporting-artists · account: user without view right
  - Steps: Sign in as a user without view right on supporting_artistes_extras_tool. → Open /film-tools/supporting-artists/my-vouchers.
  - Expected: Redirected to /film-tools.

### Rights and artiste record

- **No artiste record: every tab shows only the server message (stubbed me) @p0 @smoke** — notrun (account-hub/11-supporting-artists.spec.js:106)
  - Where: /film-tools/supporting-artists · account: user
  - Steps: Sign in as a user with view right but no artiste record (GET /api/v2/sa-portal/me → status:1, data:{}). → Visit every tab.
  - Expected: Each tab shows only the server's translated message (e.g. "No artiste record on this project for the signed-in user."); no voucher, query or profile UI. After the AD registers the user, the next refetch of me lifts the gate.

### Voucher portal (/voucher-portal/:token)

- **The code gate comes first; a signed-in colleague is not logged out @p0 @smoke** — notrun (account-hub/11-supporting-artists.spec.js:530)
  - Where: /film-tools/supporting-artists
  - Steps: In a fresh context open /voucher-portal/<token>. → Open it where a colleague is signed in.
  - Expected: Gate header "Supporting Artiste Portal", badge "Voucher", intro "This voucher is yours alone. …"; no GET /sa-portal/voucher-portal/<token> before verifying; the colleague isn't logged out or sent to /login.

- **Verify and load (stubbed request-otp / verify-otp / voucher) @p0 @smoke** — notrun (account-hub/11-supporting-artists.spec.js:554)
  - Where: /film-tools/supporting-artists
  - Steps: Enter the e-mail on file → "Send Verification Code". → Enter the code → "Verify & Continue".
  - Expected: POST /sa-portal/voucher-portal/<token>/request-otp {email} and /verify-otp {email, otp}; sessionStorage sa_voucher_portal_session:<token>; then GET /sa-portal/voucher-portal/<token> ("Loading voucher…").

- **Verify and load (real e-mail) @p0** — notrun (account-hub/11-supporting-artists.spec.js:594)
  - Where: /film-tools/supporting-artists
  - Steps: Enter the e-mail on file → "Send Verification Code". → Enter the code → "Verify & Continue".
  - Expected: POST /sa-portal/voucher-portal/<token>/request-otp {email} and /verify-otp {email, otp}; sessionStorage sa_voucher_portal_session:<token>; then GET /sa-portal/voucher-portal/<token> ("Loading voucher…").

- **Sign with a drawn signature (stubbed session, upload and sign) @p0 @smoke** — notrun (account-hub/11-supporting-artists.spec.js:599)
  - Where: /film-tools/supporting-artists
  - Steps: "Add signature" → "Sign your voucher" → draw → "Use this signature". → Tick both consents → "Sign & Submit Voucher".
  - Expected: POST /sa-portal/voucher-portal/<token>/upload (multipart) then /sign {consent_accuracy, consent_esign, sign_status:'e-sign', sign_attachment, typed_name}; panel "This voucher is signed. Payroll will process it with the next run — there is nothing further for you to do."; AD sees "✓ Signed".

- **Sign with a drawn signature (real link; AD sees ✓ Signed, 2U) @p0** — notrun (account-hub/11-supporting-artists.spec.js:637)
  - Where: /film-tools/supporting-artists
  - Steps: "Add signature" → "Sign your voucher" → draw → "Use this signature". → Tick both consents → "Sign & Submit Voucher".
  - Expected: POST /sa-portal/voucher-portal/<token>/upload (multipart) then /sign {consent_accuracy, consent_esign, sign_status:'e-sign', sign_attachment, typed_name}; panel "This voucher is signed. Payroll will process it with the next run — there is nothing further for you to do."; AD sees "✓ Signed".

### 01 Shell, access and cross-cutting › Routing, entry modes and access › Route families and redirects

- **Entitled department user lands on "All POs"** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: ADMIN
  - Steps: Log in as ADMIN (or a crew user with purchase_order_tool posting). → Open /film-tools/account-hub. → As CREW-HUB open /film-tools/account-hub/purchase-orders/all-pos.
  - Expected: 2: bare PO path, "All POs" tab active once rights load. 3: CREW-HUB gets "Approval Queue", not All POs.

- **Hidden and unknown hub paths fall through to the landing** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: ACC-PA
  - Steps: As ACC-PA open /film-tools/account-hub/dashboard. → Repeat with /journal-ledger, /gov-reporting, /ledger-properties, /xyz.
  - Expected: Each replaces to /film-tools/account-hub/purchase-orders/queue/my; no Dashboard or Journal Ledger screen renders.

- **Legacy PO link redirects into the hub** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: ACC-PA
  - Steps: As ACC-PA open /film-tools/PurchaseOrder.
  - Expected: Lands on /film-tools/account-hub/purchase-orders, accountant console view with the hub sidebar (not tool mode).

- **Legacy analytics and talent-deals links redirect** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Open /film-tools/account-hub/analytics/foo. → In Classic view open /film-tools/deal-memo/talent-deals/abc.
  - Expected: 1 → /film-tools/cost-report/analytics. 2 → /film-tools/talent-deals/abc (Talent Deals tool, not Deal Memo).

- **Full-bleed report pages render without the hub sidebar** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: As ACC-PA click sidebar "Cost Report". → Repeat for "Trial Balance", "Bible Report", "Period Close", "Tax Filing".
  - Expected: URL /film-tools/account-hub/<page>; the "Account Hub" sidebar card is gone; the page fills the full width.

- **Payroll landing and Entry Setup keep the sidebar; tiles do not** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: As ACC-PA click sidebar "Payroll". → Open /film-tools/account-hub/payroll/config. → Open /film-tools/account-hub/payroll/processing. → Repeat 2 in Windowed view.
  - Expected: 1 and 2 show the hub sidebar; 3 renders full-bleed with no sidebar; 4 opens Entry Setup (no bounce, no tile fallback).

- **Standalone tools mount without the hub sidebar** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: ACC-PA
  - Steps: As ACC-PA open /film-tools/deal-memo, /film-tools/talent-deals, /film-tools/timecard, /film-tools/cost-report, /film-tools/asset-report.
  - Expected: Each renders with no "Account Hub" sidebar; where the module header carries a theme toggle it is the round one (aria-label "Toggle colour theme").

- **Main side menu force-collapses on AH, AD and SA entry** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Expand the main side menu on /home. → Open /film-tools/account-hub. → Expand the side menu, then click another hub sidebar item. → Open /film-tools/deal-memo.
  - Expected: 2: collapsed to icons. 3: stays expanded while moving inside the hub. 4: Deal Memo does not force a collapse. /film-tools/ad-dashboard and /film-tools/supporting-artists collapse like 2.

### 01 Shell, access and cross-cutting › Routing, entry modes and access › Film Tools tiles and entry mode

- **Card Expenses tile gives an accountant the crew view** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: As ACC-PA click the Card Expenses tile. → Back in the hub, click sidebar "Production Expense Cards".
  - Expected: 1: header "Production Expense Cards" with "View your receipts, card details, and approval status.", tab bar, no card sidebar, no hub sidebar. 2: accountant layout with the card module's own sidebar (back button aria-label "Back to dashboard").

- **Cash Expenses tile gives an accountant the crew view** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: As ACC-PA click the Cash Expenses tile. → Back in the hub, click "Petty Cash Expenses".
  - Expected: 1: crew tabs only, no accountant or senior sign-off tabs, no hub sidebar. 2: accountant tabs with the hub sidebar.

- **Payroll tile vs hub "Payroll" for an accountant** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: ACC-PA click the Payroll tile
  - Steps: As ACC-PA click the Payroll tile. → Open the hub and click sidebar "Payroll".
  - Expected: 1: producer tiles, no "Payroll Entry Setup" tile, no hub sidebar. 2: accountant tiles including "Payroll Entry Setup", sidebar visible. Same in Windowed view.

- **Windowed view: a tile opens a tool-mode window** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Windowed view, as ACC-PA click the Purchase Order tile. → Switch a tab inside the window.
  - Expected: A maximized window titled with the tile name opens; inside it the department PO view, no hub sidebar, before and after the tab switch (ZL-20500); browser URL stays /workspace.

### 01 Shell, access and cross-cutting › Routing, entry modes and access › Roles and accountant-only routes

- **Admin outside accounts is treated as a department user** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: ADMIN
  - Steps: As ADMIN open /film-tools/account-hub/vendors. → Open /film-tools/account-hub.
  - Expected: 1 → department PO view. 2 → bare PO path, "All POs" tab, no hub sidebar.

- **Invoices route picks the view by role** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: ACC-PA
  - Steps: As ACC-PA open /film-tools/account-hub/invoices. → As CREW-HUB open the same URL.
  - Expected: 1: accountant Invoices module with its own sidebar (title "Invoices", back aria-label "Back to dashboard"), hub sidebar hidden. 2: department invoice view, no sidebar.

- **Bank Reconciliation has no role gate** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: CREW-HUB
  - Steps: As CREW-HUB open /film-tools/account-hub/bank-reconciliation.
  - Expected: Product intent (module and badge helpers call it accountant-only): redirect like AH-SH-019. Code: the module renders; neither route tree checks isAccountant. Record what the backend returns.

- **PA / FC are senior without a Settings → Team row** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Remove ACC-PA from card Settings → Team (or confirm absent). → ACC-PA opens "Production Expense Cards". → ACC-JR does the same.
  - Expected: ACC-PA sees "Settings" in the card sidebar; ACC-JR does not.

- **Pending member makes no Production Setup request** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: PENDING
  - Steps: Log in as PENDING. → Open the Deal Memo tile.
  - Expected: Their own deal opens; no GET /api/v2/account-hub/project-settings is sent (it answers 406 for a pending member).

### 01 Shell, access and cross-cutting › Routing, entry modes and access › Payroll gate

- **Payroll Entry Setup is accountant-only** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: PROD
  - Steps: As PROD open /film-tools/account-hub/payroll/config.
  - Expected: Replaced to /film-tools.

- **Hub-entry payroll URL needs account_hub_tool** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: As PROD type /film-tools/account-hub/payroll (no ?entry=tool).
  - Expected: Gate requires account_hub_tool on hub entry → replaced to /film-tools.

### 01 Shell, access and cross-cutting › Routing, entry modes and access › View gate and live revocation

- **Nothing loads while access is denied** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Record network as CREW-TOOL. → Open /film-tools/account-hub/vendors.
  - Expected: Replaced to /film-tools; no GET …/project-settings, GET /v2/departments?designations=false or GET /api/v2/vendors… is sent; no module content flashes.

- **Live revocation in Windowed view closes only affected windows** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Windowed view: ACC-JR has an Account Hub window and a Deal Memo window open. → ADMIN revokes only ACC-JR's Account Hub viewing right.
  - Expected: The Account Hub window closes; the Deal Memo window stays open and usable.

- **Removing a tool in Tool Customization ejects users** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: ACC-JR sits on /film-tools/timecard. → ADMIN removes Time Card in Tool Customization. → Restore.
  - Expected: ACC-JR is replaced to /film-tools (a missing row counts as revoked).

- **Gate fails open while rights are loading** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: ACC-PA
  - Steps: Clear persisted app state. → page.route **/v2/project/tools to hang. → As ACC-PA open /film-tools/account-hub.
  - Expected: Hub renders while the rights request is pending; no flash redirect to /film-tools.

- **Legacy links lose the entry mode** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: CREW-TOOL
  - Steps: As CREW-TOOL open /film-tools/PurchaseOrder. → As ACC-JR without cost_report_tool open /film-tools/account-hub/analytics.
  - Expected: Code today: both end on /film-tools. 1 is judged as hub entry (needs account_hub_tool); 2 lands on /film-tools/cost-report/analytics (needs cost_report_tool). Confirm whether intended.

### 01 Shell, access and cross-cutting › Hub chrome: sidebar, windows, theme and scroll › Sidebar content and visibility

- **Sections and items in order** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/* · account: ACC-PA
  - Steps: As ACC-PA open the hub. → Read the sidebar top to bottom.
  - Expected: Theme card, then Setup (Guide, Production Setup) · Transactions (Purchase Orders, Invoices / Accounts Payable, Production Expense Cards, Petty Cash Expenses) · Payroll Management (Payroll) · Reports (Cost Report, Period Close) · Management (Vendors, Trial Balance, Bible Report, Bank Reconciliation, Tax Filing) · Configuration (Approvers, Budget, Chart of Accounts, Forms Configuration). No Dashboard, Journal Ledger, Gov. Reporting, Ledger Properties, Deal Memo or Time Card item. Section labels display uppercase (CSS).

- **Self-nav modules replace the hub sidebar** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: As ACC-PA click "Invoices / Accounts Payable". → Click the module back button (aria-label "Back to dashboard"). → Repeat with "Production Expense Cards".
  - Expected: Hub sidebar disappears and the module's own sidebar shows; back goes through /film-tools/account-hub/dashboard to …/purchase-orders/queue/my with the hub sidebar again.

- **The sidebar is not filtered by module rights** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Remove ACC-JR's card_expenses_tool view; keep account_hub_tool. → ACC-JR opens the hub and clicks "Production Expense Cards". → Restore.
  - Expected: Item listed; the page opens and stays (hub entry needs only account_hub_tool).

### 01 Shell, access and cross-cutting › Hub chrome: sidebar, windows, theme and scroll › Classic and windowed navigation

- **Classic view: the sidebar navigates in place** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Classic view, ACC-PA in the hub. → Click "Vendors", then "Approvers". → Open any PO sub-tab.
  - Expected: URL changes in the same page; the sidebar stays mounted; on first load of a module an inline spinner "Loading…" shows in the content area; the clicked item gets the orange active rail; every /purchase-orders/* URL keeps "Purchase Orders" active.

- **Windowed view: the sidebar opens a module window** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Windowed view, open the Account Hub tile. → Click "Vendors" in its sidebar. → Click "Vendors" again.
  - Expected: A new maximized window titled "Vendors" opens (with its own hub sidebar); the first window stays on Purchase Orders. The second click restores the same window; no duplicate.

- **Windowed view: back button closes the window** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: In an Account Hub window click the back button (aria-label "Back to film tools").
  - Expected: That window closes; the desktop and other windows remain.

- **Two Account Hub windows at once** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Windowed view, set the AH theme to dark. → Open the Account Hub tile and the Deal Memo tile. → Navigate inside each.
  - Expected: Both windows work independently; the second window opens dark on its own root (not left light).

### 01 Shell, access and cross-cutting › Hub chrome: sidebar, windows, theme and scroll › Theme and loader

- **Sidebar toggle switches and persists the theme** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: ACC-PA in the hub; theme card shows "Light mode" and "Click to switch". → Click it. → Reload. → Click again.
  - Expected: 2: label "Dark mode"; #account-hub-root has class dark; localStorage['accounthub-theme'] === 'dark'; <html> colour-scheme dark. 3: still dark. 4: back to light.

- **The round toggle shows only without the hub sidebar** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: ACC-PA on the hub PO page. → ACC-PA via the Purchase Order tile. → CREW-HUB on PO. → ACC-PA on /film-tools/deal-memo.
  - Expected: 1: no round toggle in the page header. 2–4: round toggle (aria-label "Toggle colour theme", tooltip "Switch to dark mode" / "Switch to light mode") drives the same stored theme.

- **Leaving Account Hub restores the light colour scheme** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Set the AH theme to dark. → Navigate to /home. → Return to the hub.
  - Expected: 2: document.documentElement.style.colorScheme === 'light'; the rest of the app is not dark. 3: dark again.

- **Theme syncs across browser tabs** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Open the hub in two tabs of one context. → Toggle in tab A.
  - Expected: Tab B flips to the same theme without a reload.

- **AD Dashboard and Supporting Artists keep their own theme** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Set the AH theme to dark. → Open /film-tools/ad-dashboard, then /film-tools/supporting-artists.
  - Expected: #account-hub-root has no dark class on either; each follows its own toggle; shared pickers inside follow that tool's theme.

- **Themed loader on Tax Filing, no light first paint** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Set the AH theme to dark. → Delay JS chunk responses with page.route. → Open /film-tools/account-hub/tax-filing. → Without delay, reload a dark hub page and check the first frame.
  - Expected: 3: loading canvas #14161b with the spinner, not a white page (other AH routes still use the plain loader). 4: .dark present on the first painted frame.

- **Two windows drift apart after a toggle** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Windowed view, light theme: open Account Hub and Deal Memo windows. → Toggle dark in the Account Hub sidebar. → Look at the Deal Memo window.
  - Expected: Product intent: one shared AH theme. Code: each window has its own ThemeProvider state and the storage event does not fire in the same document, so Deal Memo likely stays light until reopened.

### 01 Shell, access and cross-cutting › Hub chrome: sidebar, windows, theme and scroll › Scroll reset

- **Content scroll resets on module switch** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/*
  - Steps: Classic view, ACC-PA scrolls a long PO list to the bottom. → Click "Vendors".
  - Expected: Vendors opens at the top (.ah-content scrollTop 0), no visible jump.

### 01 Shell, access and cross-cutting › Shared data, Production Setup tour, Guide and translations › Providers and reference data

- **One settings fetch per entry; refresh on module switch** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: Record network; ACC-PA opens the hub. → Within 2 s click "Invoices / Accounts Payable". → Wait over 2 s, click "Purchase Orders".
  - Expected: One GET /api/v2/account-hub/project-settings on entry; none for step 2; one more for step 3; no loader re-shown on the refresh.

- **Departments retry, then give up** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: Fulfil **/v2/departments?designations=false with {status:0}. → Install page.clock; open the hub. → Advance 2 s, 8 s, 30 s.
  - Expected: Four calls in total (initial plus retries at 2 s, 8 s, 30 s), then none; page still usable with empty department pickers. Unblocking before the last retry fills them.

- **Departments keep admin order and follow a reorder live** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: ACC-PA opens a department picker (e.g. the PO department filter). → ADMIN changes Settings → department listing order.
  - Expected: Order matches the admin order, not alphabetical; after the reorder it updates without reload or loader.

- **Reference-data failures don't break the hub** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: Fulfil /v2/preset/isd-codes with 500. → Fulfil /api/v2/account-hub/project-settings with 500. → Open the hub; open a country picker (e.g. Vendors → add vendor).
  - Expected: Country picker still lists countries (bundled fallback); modules render with empty settings; no Production Setup tour; no crash.

- **Another user's Production Setup save refreshes silently** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: ACC-JR has a hub page open. → ACC-PA adds a company in "Production Setup". → Delete it afterwards.
  - Expected: ACC-JR's page sends a new GET …/project-settings within about 1 s of the production_setup:updated frame, with no loader; the company becomes selectable.

### 01 Shell, access and cross-cutting › Shared data, Production Setup tour, Guide and translations › Project switch

- **Switching project drops cached data** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: ACC-PA opens a nominal-code picker (e.g. a PO line) in PROJ-A. → Change project to PROJ-B; open the same picker.
  - Expected: A fresh GET /api/v2/account-hub/chart-of-accounts… fires; only PROJ-B codes show.

- **The tour seen-flag is per project** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: Dismiss the tour in PROJ-B. → Switch to PROJ-C and open the hub.
  - Expected: Tour shows again in PROJ-C; ah_setup_tour_seen holds {"<PROJ-B id>":{"production":true}}.

### 01 Shell, access and cross-cutting › Shared data, Production Setup tour, Guide and translations › Production Setup tour

- **Intro modal on an unconfigured project** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: Clear ah_setup_tour_seen. → ACC-PA opens the hub in PROJ-B.
  - Expected: About 0.8 s after load a modal "Account Hub — Production Setup" with "First we need to set up a few things in the Account Hub before starting work in any other module." and a "Next →" button.

- **Steps follow the guide order and point at the sidebar** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: In AH-SH-063 click "Next →". → Step through the tour.
  - Expected: Steps: "Companies & Entities", "Project Currencies", "Account Tags", "Tax Types", then "Chart of Accounts" if the chart is empty. "Bank Accounts" appears only when the settings doc carries a bank list that is empty. Steps highlight the sidebar "Production Setup" item; the chart step highlights "Chart of Accounts"; a segmented progress bar shows the position.

- **Closing the intro or the tour marks it seen** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: Close the intro with ×, Escape or the backdrop (repeat with finishing or closing the tour). → Reload and open other hub modules.
  - Expected: Tour never returns for this project; ah_setup_tour_seen["<projectId>"].production === true.

- **A configured project never sees the tour** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: ACC-PA opens the hub in PROJ-A with the flag cleared. → Wait 2 s.
  - Expected: No intro modal and no tour bubble.

- **Where the tour stays quiet** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: PROJ-B, flag cleared: ACC-PA opens /film-tools/account-hub/production-setup directly. → ACC-PA opens via the Purchase Order tile. → ACC-PA opens "Invoices / Accounts Payable". → CREW-HUB opens the hub.
  - Expected: No tour in any of them (production-setup path suppressed; no hub sidebar in 2–4). Afterwards, ACC-PA on any other hub module does get it.

- **A failed chart-of-accounts fetch reports a false gap** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: PROJ-A, flag cleared. → Fulfil /api/v2/account-hub/chart-of-accounts with 500. → Open the hub.
  - Expected: Expected: no tour (unknown is not missing). Code: useCoaRows swallows the error, so a tour with one "Chart of Accounts" step likely fires.

- **The tour follows dark mode** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: Dark theme, PROJ-B, flag cleared. → Open the hub and start the tour. → Toggle the theme mid-tour.
  - Expected: Intro and bubble are dark with light text; the toggle keeps the current step.

### 01 Shell, access and cross-cutting › Shared data, Production Setup tour, Guide and translations › Guide page

- **Guide layout** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: ACC-PA clicks sidebar "Guide".
  - Expected: Eyebrow "Setup", title "Guide", text "How each Account Hub module works. Guides open in a new tab."; 16 cards: Setup (Production Setup), Transactions (4), Payroll Management (Payroll), Reports (Cost Report, Period Close), Management (Vendors, Trial Balance, Bible Report, Bank Reconciliation), Configuration (4). No card for Guide or Tax Filing. Each card has "watch video" (shown capitalised) and "Read More".

- **Card buttons open the docs and the video** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide
  - Steps: Click "Read More" on "Vendors". → Click "watch video" on "Vendors".
  - Expected: 1: new tab https://documentation.zillit.com/?project_type=<project type or default>#account-hub-vendors, opened with noopener. 2: the Tools video player modal plays the Vendors video.

### 01 Shell, access and cross-cutting › Shared data, Production Setup tour, Guide and translations › Translations

- **A French browser sees the translated shell** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide · account: ACC-PA
  - Steps: New context with locale: 'fr-FR'; log in as ACC-PA. → Open the hub; toggle to dark.
  - Expected: Section "Écritures" (Transactions); back button aria-label "Retour aux outils cinéma"; theme card "Mode sombre"; no raw ah_… key anywhere in the sidebar.

- **French "Payroll" sidebar item** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide · account: AH-SH-072
  - Steps: As AH-SH-072, read Payroll Management.
  - Expected: payroll_label is missing from fr.js: the item shows the server label if the label service sends one, otherwise the raw text payroll_label.

- **Strings still hard-coded in English** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: /film-tools/account-hub/guide · account: accountant
  - Steps: fr-FR, PROJ-B, flag cleared: open the hub. → As accountant open an invoice in Entry and press Post with no vendor. → Switch hub modules in Classic view on a slow network.
  - Expected: Tour intro and steps stay English ("Account Hub — Production Setup", "Companies & Entities"…); the alert title "Action needed" and button "OK" stay English (its message is translated); the inline "Loading…" stays English.

### 01 Shell, access and cross-cutting › Realtime refresh and badges (socket events → ah:*) › Socket refetch

- **Another user's change refreshes an open list** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: ACC-JR opens sidebar "Vendors". → ACC-PA creates a vendor in another context. → Delete it afterwards.
  - Expected: Within 1–2 s ACC-JR's page sends GET /api/v2/vendors… and shows the vendor, with no loader and no toast.

- **Frames from another project are ignored** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: ACC-JR on Vendors in PROJ-A. → ACC-PA creates a vendor in another project.
  - Expected: No vendors refetch on ACC-JR's page; the console shows "[AH socket] DROPPED cross-project".

- **An approval-tier change refreshes the affected queue** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: CREW-HUB (a cash approver) has the Petty Cash approval queue open. → ACC-PA edits the Petty Cash tiers in "Approvers". → Revert.
  - Expected: The queue and cash metadata refetch without reload.

- **A chart-of-accounts change refreshes open pickers** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: ACC-JR has a nominal-code picker on screen. → ACC-PA adds a code in "Chart of Accounts". → Deactivate it afterwards.
  - Expected: The new code appears in ACC-JR's picker without reload.

- **Socket debug logging in the console** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Watch the console as ACC-PA. → Trigger any AH event (e.g. AH-SH-075).
  - Expected: Code logs "[AH socket] registerAccountHubListeners attached for N events" and every envelope, payload included, before filtering (marked temporary). Confirm whether this ships to production.

### 01 Shell, access and cross-cutting › Realtime refresh and badges (socket events → ah:*) › Badges and badge clearing

- **Sidebar badge items and overflow** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Create unread notifications for ACC-PA in PO, invoices, card, cash and bank reconciliation. → Open the hub.
  - Expected: Red count badges only on "Purchase Orders", "Invoices / Accounts Payable", "Production Expense Cards", "Petty Cash Expenses", "Bank Reconciliation"; counts above 99 show "99+". Petty Cash ignores notification-only areas (payment routing, history, settings).

- **A new notification bumps the sidebar live** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: ACC-PA in the hub. → CREW-HUB submits something that badges an accountant (e.g. a petty cash claim batch).
  - Expected: The matching sidebar badge increments without reload.

- **A failed approval keeps the badge** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting · account: AH-SH-082
  - Steps: As AH-SH-082, but fulfil the approve request with an error. → Approve.
  - Expected: The server's error message shows as a toast; the badge count is unchanged.

- **Query is hidden on approval queues** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: CREW-HUB opens a PO from "Approval Queue". → Opens the same PO from "My POs" (if theirs) or "All POs".
  - Expected: The approval-queue detail offers no Query; the other tab's detail does.

- **An open query thread reads new messages** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: ACC-PA opens a PO's Query panel. → CREW-HUB replies in that thread.
  - Expected: The reply appears live and ACC-PA's query badge for that PO stays cleared while the panel is open.

### 01 Shell, access and cross-cutting › Public portal OTP gate (/deal-memo/crew-portal/:token and the other portals) › First entry and code request

- **"Send Verification Code" waits for a valid email** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Type abc. → Type  abc@example.com . → Press Enter.
  - Expected: Disabled for 1, enabled for 2; Enter sends POST …/request-otp with the trimmed email; button reads "Sending…" while busy.

- **A server refusal shows the server's message** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Fulfil request-otp with an error whose message is "This email address does not match the one on file for this deal." → Send.
  - Expected: Stays on step 1 showing exactly that text, with no extra title or raw key; button still offered.

- **No email on file is a dead end** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Fulfil request-otp with message: "deal_portal_otp_no_email". → Send.
  - Expected: Notice shows the server text; the email field is disabled and "Send Verification Code" disappears.

- **Network failures don't blame the recipient** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Abort request-otp; Send. → Stub a successful request, then abort verify-otp; verify.
  - Expected: 1: "Couldn't send the code." + "Something went wrong reaching the server. Check your connection and try again." + raw "Network Error". 2: "Couldn't check that code." + the same body; not a wrong-code message; boxes cleared.

### 01 Shell, access and cross-cutting › Public portal OTP gate (/deal-memo/crew-portal/:token and the other portals) › Code entry

- **Code boxes: paste, advance, backspace, Enter** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: On step 2 paste 123456 into box 1. 2. Clear; type digits one by one, a letter, and Backspace on an empty box. 3. Press Enter with all six filled.
  - Expected: Paste fills all six; typing advances focus; letters are ignored; Backspace on an empty box moves back; "Verify & Continue" is enabled only when full; Enter submits.

- **Wrong code** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Fulfil verify-otp with message: "deal_portal_otp_invalid" (or type a wrong real code). → Verify.
  - Expected: Stays on step 2 with the server message; boxes cleared and box 1 focused.

- **Resend cooldown and failed resend** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: On step 2 note "Resend in 30s". → Advance the clock 30 s. → Fulfil request-otp with an error message; click "Resend code".
  - Expected: 1: counts down; 2: "Resend code" appears; 3: back on step 1 with the server's reason and the email still typed.

- **Code expiry** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: On step 2 advance the clock 15 min.
  - Expected: "Code expired"; "Verify & Continue" disabled; "Resend code" offered.

- **"‹ Change email"** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: On step 2 click "‹ Change email".
  - Expected: Back on step 1 with the email still typed and no notice.

### 01 Shell, access and cross-cutting › Public portal OTP gate (/deal-memo/crew-portal/:token and the other portals) › Session

- **Session survives reload, not a new tab or its deadline** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: After AH-SH-097, reload. → Open the link in a new tab. → Set dm_portal_session:<token> expiresAt in the past; reload.
  - Expected: 1: straight to the portal. 2: step 1 (sessionStorage is per tab). 3: step 1, and the stale entry is removed.

- **A lost session overlays the page without losing work** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Verified; start editing the crew details form. → Make the next gated call return message: "deal_portal_otp_session_expired". → Verify again.
  - Expected: Verify card appears as a full-screen overlay; the form beneath keeps its values; after verifying the overlay closes and the edits are still there.

- **Wording of the re-gate notice** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Verified; advance the clock past expires_at (or trigger AH-SH-099).
  - Expected: Code: the notice shows "Couldn't send the code." / "Something went wrong reaching the server…" plus the raw key deal_portal_otp_session_expired: a network-failure message for a timeout. Confirm with product.

- **Portal calls use header auth, never cookies** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Verify with a verify-otp body that includes token. → Capture the next gated request.
  - Expected: It carries x-portal-otp and Authorization: Bearer <token>; no credentials mode include.

- **The same gate guards every external portal** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Logged out, open /deal-memo/nda-portal/<t>, /extras-portal/<t>, /voucher-portal/<t>, /guarantor-portal/<t> (any token).
  - Expected: Each shows step 1 ("STEP 1 OF 2", "Verify Your Identity") with its own header label (e.g. "NDA Portal") and calls no gated endpoint first.

- **The in-app Supporting Artistes tool never asks for a code** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: A signed-in user with supporting_artistes_extras_tool opens /film-tools/supporting-artists.
  - Expected: Tool opens directly; no "Verify Your Identity" screen.

- **Portal copy follows the browser language** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: 01 Shell, access and cross-cutting
  - Steps: Logged out, locale: 'fr-FR', open a crew-portal link.
  - Expected: Gate text in French, e.g. "Vérifiez votre identité" and badge "Portail de l’équipe".

### 01 Shell, access and cross-cutting › Shared UI kit behaviours › Toasts, alerts, confirms and money inputs

- **Toasts show the server's message** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: src/accountHub/components/ui/ · account: ACC-PA save a vendor edit
  - Steps: As ACC-PA save a vendor edit. → Repeat with the save fulfilled as {status:0,message:"<some key>"}.
  - Expected: A success toast, then an error toast, each showing the server's message through the dictionary (the raw key if the dictionary lacks it), above any open modal or MDI window.

- **The blocking alert waits for "OK"** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: src/accountHub/components/ui/ · account: accountant
  - Steps: As accountant open an invoice in Entry. → Press Post with no vendor selected.
  - Expected: Modal titled "Action needed" with "Select a vendor before posting to the ledger" and one "OK" button; stays until OK, × or backdrop; nothing posts.

- **Confirm dialog before a destructive action** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: src/accountHub/components/ui/
  - Steps: Production Setup → Bank Accounts: click a row's delete. → Click "Cancel". → Delete again and click "Delete".
  - Expected: Dialog "Remove bank account" with "Remove "<name>" ending <last4>? This can't be undone."; Cancel, × and backdrop close without deleting; "Delete" shows "Deleting…" while busy and removes the row once.

- **Money inputs group and calculate** — planned (e2e/account-hub/01-shell-access-and-cross-cutting.md)
  - Where: src/accountHub/components/ui/
  - Steps: In a PO line amount type 1500. → Type 100+50*2, then Tab. → Type 12a.
  - Expected: 1: shows 1,500 while typing, commits 1,500.00. 2: preview = 200.00 while typing, commits 200.00 on blur. 3: the letter is refused.

### 02 Setup and configuration › Production Setup / Accounting Setup › Access & navigation

- **Same gate in a tool window** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: As B, open Account Hub in an MDI window. → Navigate the window to production-setup.
  - Expected: The window lands on Purchase Orders; as A it shows Production Setup.

- **Card order on the full page** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: As A, open the page. → Read the card titles top to bottom.
  - Expected: "Companies / Entities", "Bank Accounts", "Project Currencies", "Account Tags", "Tax Types", then "Payroll Entry Setup", "Time Card Entry Setup", "Purchase Order Entry Setup", "Invoices Entry Setup", "Production Expense Cards Entry Setup", "Petty Cash Entry Setup".

- **Requests on mount** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Start network capture. → Open the page.
  - Expected: One GET /account-hub/project-settings (the page and the shared provider share it), GET /account-hub/bank-accounts?entity_type=production&per_page=200, GET {projectBase}/v2/preset/taxes-by-country.

- **Non-accountant uses the Settings view** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: As B, open /settings/production-setup. → "Add company" › open "Add bank account" in the modal.
  - Expected: Page renders (no gate). In the bank modal the two nominal-code labels have no red asterisk and the bank saves with both codes blank.

- **Settings view layout and chrome** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Open /settings/production-setup with several companies. → Scroll.
  - Expected: The page scrolls inside main.ah-content (nothing clipped); the main side menu stays and highlights Settings.

### 02 Setup and configuration › Production Setup / Accounting Setup › Companies: create and validation

- **Empty companies list** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub GET /account-hub/project-settings with companies: []. → Open the page.
  - Expected: Card text "No companies added yet. Click Add company to register the production's legal entities."

- **Legal name follows the name unless ticked** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Create with the tick off. → Edit, tick it, type "E2E Legal Ltd", "Done". → Edit another, tick it, leave it empty, "Done".
  - Expected: Case 1 saves legal_name = name. Case 2 saves "E2E Legal Ltd" and reopens ticked with that value. Case 3 saves the trading name and doesn't block.

- **Country search** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: In "Country", type "GB". → Clear, type "germ".
  - Expected: Options come from GET …/preset/isd-codes; each shows its ISO code; "GB" finds United Kingdom, "germ" finds Germany; placeholder "Type country name or ISO code…".

- **Tax credit tags** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: In "Tax credit tagging" type "UK HETV" + Enter. → Type "uk hetv, AVEC" + Enter. → Press Backspace in the empty input. → Save.
  - Expected: First chip added; "uk hetv" ignored as a duplicate (case-insensitive), "AVEC" added; Backspace removes the last chip; placeholder changes to "Add another…"; tax_credits saved as typed strings.

- **Discard a new company** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: "Add company", type a name. → Press Escape (repeat with × and "Cancel").
  - Expected: Modal closes; no PATCH; no card added.

- **UK fields show for GB only** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Pick country United Kingdom. → Switch to France.
  - Expected: GB shows "PAYE reference", "Accounts Office reference", "Pension provider", "Pension scheme reference"; France hides all four.

- **PAYE reference format** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: GB company. → Type "12AB" in "PAYE reference". → Replace with "123/AB456". → Clear it.
  - Expected: "12AB" shows "Use the format 123/AB456." and disables submit; "123/AB456" clears the error; empty is allowed.

- **Accounts Office reference format** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: GB company. → Type "123P0001". → Replace with "123PA00012345".
  - Expected: Invalid value shows "Use the format 123PA00012345." and disables submit; the valid value enables it.

- **UK field limits and trimming** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Paste 25 characters into each PAYE field. → Type "123 /AB456". → Paste 210 chars into "Pension provider", 60 into "Pension scheme reference".
  - Expected: PAYE and AO stop at 20 characters; spaces in the two references are stripped as typed; provider stops at 200 and scheme ref at 50; provider keeps inner spaces ("Legal & General").

- **Leaving GB keeps stored UK refs** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Edit a GB company with PAYE refs. → Change country to Ireland, "Done". → Inspect the body.
  - Expected: The uk block is sent with all four keys and the stored values (not cleared).

### 02 Setup and configuration › Production Setup / Accounting Setup › Companies: edit, banks and delete

- **Edit modal** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Click a company card (or its pencil, aria-label "Edit company").
  - Expected: Modal "Edit {name}" seeded with saved values; no Cancel button; submit "Done"; red "Remove company" bottom-left.

- **Rename updates bank holders** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Edit a company that owns a bank; rename it; "Done". → Look at its bank card.
  - Expected: Companies PATCH succeeds; the bank card's "Holder" shows the new name (read live from the company, not the stored snapshot).

- **Failed company save** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub the companies PATCH with an error envelope. → Edit a company, "Done".
  - Expected: Error toast; modal stays open with the edits; the card shows the "Unsaved" pill and a "Save changes" button.

- **Currency derived from banks** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Tick two banks in different currencies in the company modal.
  - Expected: A "Currency" row shows one pill per distinct code and the text "Currency is set by the selected bank accounts."; the card shows both pills after save. No currency field is sent.

- **No unlink when the company save fails** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub the companies PATCH to fail. → Untick a bank, "Done".
  - Expected: No bank PATCH is sent; error toast; modal stays open.

- **Add a bank from the company modal** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Edit a saved company. → "Add bank account". → Fill and "Save Account". → Repeat Escape inside the bank modal.
  - Expected: Stacked "Add Bank Account" has Account Holder locked (read-only) to this company and no "+ Add company"; after save the new bank is ticked in the company modal. Escape closes only the bank modal. For an unsaved new company the holder shows the normal picker instead.

- **No banks to pick** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Open the company modal with no production banks (stub), then with every bank claimed.
  - Expected: "No production banks yet — use Add bank account to create one." / "All production banks are already assigned to other companies — use Add bank account to create another."

- **Server refuses the removal** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub the companies PATCH to fail (for example a company used by a PO). → Remove it.
  - Expected: Error toast; the card stays; the confirm stays open; the card is not marked dirty.

### 02 Setup and configuration › Production Setup / Accounting Setup › Bank accounts

- **Empty bank list** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub the bank list as empty. → Open the page.
  - Expected: Large card "Add your first bank account" with "Payroll runs and vendor disbursements default to the accounts you add here."; clicking it opens "Add Bank Account".

- **Account number filter** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Type "40-18 37A62" in "Account Number". → Paste 60 digits.
  - Expected: Field keeps only digits ("401837 62" → "40183762"); input stops at 50 characters.

- **Duplicate account number** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Add a bank whose number matches an existing bank (try with spaces).
  - Expected: "An account with this number already exists."; no POST.

- **Sort code mask** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Type "204891" in "Sort Code". → Edit a bank stored with a 9-digit sort code.
  - Expected: Shows "20-48-91" and sends "204891"; the 9-digit value shows unchanged until edited.

- **Unknown nominal code: quick-create** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: As A, type a code not in the chart (e.g. E2E9999) in "Bank Account Nominal Code", press Enter. → "Add & use". → Repeat and press "Cancel".
  - Expected: Modal "Add E2E9999 to the Chart of Accounts" with "Display name"; "Add & use" sends POST /account-hub/chart-of-accounts (line_type: "category", parent_id: null) and fills the field; "Cancel" clears the field.

- **Unknown code from a non-accountant** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup · account: B in the Settings view
  - Steps: As B in the Settings view, add a bank with nominal E2E8888 (not in the chart).
  - Expected: No quick-create modal; the POST sends nominal_code: "[[E2E8888]]".

- **Currency picker order** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Open "Currency". → Search "eur".
  - Expected: Project currencies listed first with a "Project" tag, then every other catalogue currency; search matches code, name or symbol.

- **Additional details** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: "Add Field", pick type Email, title "Remit email", "Done". → Type "bad" as value, "Save Account". → Fix the value, save.
  - Expected: Invalid value shows "Enter a valid email address." and blocks the save; valid value saves additional_details: [{ field, value, field_type: "email" }]; untitled rows are dropped.

- **"+ Add company" from the bank modal** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: In "Add Bank Account" click "+ Add company". → Create "E2E-<run>-Co2" (country).
  - Expected: Stacked "New company" modal without a Bank accounts section; on "Add company" a companies PATCH with all companies plus the new one; the holder is set to the new company; the Companies card above shows it without reload.

- **Edit a bank** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Click the bank card pencil ("Edit account"). → Change the name, "Update Account".
  - Expected: Modal "Edit Bank Account" with no Cancel; PATCH /account-hub/bank-accounts/{id}; card updates.

- **Holder company was deleted** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Remove the company that owns a bank. → Edit that bank.
  - Expected: Card "Holder" shows "—"; modal shows "Previously: {name} — select the matching company."; save blocked with "Account holder company is required." until a company is picked.

- **Bank card display** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Look at a bank card. → Click the reveal icon, wait 5 s. → Click "Copy".
  - Expected: Account number masked to the last 4; "Show" reveals it and it re-masks after 5 s; "Copy" becomes "Copied"; subline "NOM {code}" / "AP {code}"; uncoded codes show "—".

- **Server refuses a bank delete** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub the DELETE with 409. → Delete a bank.
  - Expected: Error toast; confirm stays open; the card stays.

- **Bank list fails to load** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub GET /account-hub/bank-accounts with 500.
  - Expected: A toast with the server message (or "Failed to load bank accounts"); the card shows the empty state.

- **Escape in the stacked "+ Add company"** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Fill part of "Add Bank Account". → "+ Add company". → Press Escape.
  - Expected: Expected: only the company modal closes. Code: both modals close and the bank form is lost (no closeOnEscape guard on the bank modal).

### 02 Setup and configuration › Production Setup / Accounting Setup › Project Currencies

- **Empty selection and catalogue** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Open with no currencies saved.
  - Expected: Left panel "Selected" with "No currencies selected yet" / "Pick at least one from the list →"; right "Add currencies" with "{shown} / {total}" and tiles from GET …/preset/currencies.

- **Change the default** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: In "Default currency" click EUR. → Give GBP a rate. → Save.
  - Expected: Body default: "EUR", EUR exr: 1; GBP now needs a rate before saving.

- **Remove the default / clear all** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Remove the default's card (aria-label "Remove GBP"). → "Clear all", save.
  - Expected: The next currency is promoted to default; after "Clear all" the body is { currencies: [], default: null }.

- **Search and filters** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Type "yen". → Click "Major". → Type "zzz". → Clear with ×.
  - Expected: Search matches code, name, country, symbol; "Major" shows only USD, EUR, JPY, GBP, CNY, CHF, CAD, AUD; "No currencies match "zzz"."; × ("Clear search") restores the list.

- **Failed currency save** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub the PATCH to fail. → Save.
  - Expected: Error toast; selection and rates stay; "Save changes" still shown.

### 02 Setup and configuration › Production Setup / Accounting Setup › Account Tags

- **Tag input behaviour** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Type "camera" + Enter. → Type "a1, b2" + Enter. → Type "CAMERA" + Enter. → Backspace in the empty input. → Click "+ VFX" under "Suggested".
  - Expected: Chips "CAMERA", "A1", "B2" (uppercased while typing); duplicate ignored; Backspace removes the last chip; "VFX" added and gone from suggestions; caption "Tags · {n}".

### 02 Setup and configuration › Production Setup / Accounting Setup › Tax Types

- **Toggle taxes and countries** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Click one tax row. → Click the group checkbox twice. → Remove the country chip.
  - Expected: Row toggles; group checkbox shows partial, then all off/on; removing the country unchecks all its taxes.

- **Reclaimable and nominal per tax** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: On a checked tax set "Reclaimable status" to "Reclaimable". → Pick a Nominal code.
  - Expected: Select defaults to "Non-reclaimable"; using the select or the nominal input does not toggle the row.

- **Custom tax rows** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: "Add custom tax type". → Fill "Tax", "Rate description", value "5". → Add a second row with no label. → Save.
  - Expected: Header row "Type / Label / Value (in %) / Reclaimable / Nominal"; the labelled row saves with identifier: "custom_N", country: null; the unlabelled row is dropped.

- **Custom rate range** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Custom value "101", save. → Value "-1". → Value "100".
  - Expected: 101 and -1 get a red border, tooltip and toast "Tax rate must be between 0% and 100%."; no PATCH; 100 saves.

- **Narrow viewport** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Resize to 800 px wide.
  - Expected: Tax rows and the custom grid scroll horizontally; Reclaimable and Nominal stay reachable.

### 02 Setup and configuration › Production Setup / Accounting Setup › Entry Setup tiles

- **Tile chrome and modal shell** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Click "Configure" on each tile.
  - Expected: Each opens a two-pane modal with a section list, "{i} / {n}", "Close", "Save changes" (disabled until something changes) and "Unsaved changes" when dirty; footer hints "Esc to close" and "⌘S to save".

- **Closing drops unsaved edits** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Change a setting. → Press Escape (then repeat with a backdrop click). → Re-open. → Change again and press Ctrl/⌘+S.
  - Expected: Escape and backdrop close with no prompt and the change is gone; ⌘S saves only when dirty.

- **Purchase Order tile loads** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: "Configure" on "Purchase Order Entry Setup".
  - Expected: GET /purchase-orders/settings and GET /account-hub/assignment-rules?module=purchase_orders; sections "Description Format", "Rental & Split", "Issuance", "Asset Register Rules", "Auto-Assignment Rules"; "Require effective date" and "Enforce period close" show "Always".

- **Asset price range** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: "Asset Register Rules" › "Price range" 500 to 100. → Save.
  - Expected: Inline error and toast "Price low must not exceed price high."; no PATCH.

- **Auto-assignment rules** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: "Add rule", set Departments, a vendor, Amount min, "Assign to". → Save. → Remove the rule ("Remove rule"), save.
  - Expected: Vendor options from GET /vendors?limit=500; POST /account-hub/assignment-rules with module: "purchase_orders"; then DELETE /account-hub/assignment-rules/{id}. A rule whose assignee left Accounts shows "Pick assignee…".

- **Invoices tile** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Configure "Invoices Entry Setup". → "Add member", set "Posting limit", tick "Senior". → "Run Authorization" › "Add first level". → Save.
  - Expected: GET then PATCH /invoices/settings with team_members and run_authorization; no success toast appears (see defects).

- **Production Expense Cards tile** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Configure the cards tile. → Toggle "Accountant Override — Receipts". → Enable "Fuel — Personal Use Deduction". → Set a Quick Code nominal. → Save.
  - Expected: Sections "Accounts & Custodian", "Team & Posting Rights", "Department Coordinators", "Approval Overrides", "Deduction Rules", "Quick Codes", "Auto-Assignment Rules"; PATCH /card-expenses/settings; success toast. Coordinator picker lists every department, Accounts included.

- **Petty Cash tile** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Configure the petty cash tile. → Set "Float custodian account" from the BS code picker. → Toggle "Senior Sign-off Required". → Save.
  - Expected: PATCH /cash-expenses/settings with float_custodian_account and the toggle; success toast.

- **Payroll tile** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Configure "Payroll Entry Setup". → Add an approver from "Accounts team — available to add". → "Week starts on" Wednesday. → Save.
  - Expected: Picker lists accounts users only; a non-7-day window shows the warning; PATCH /account-hub/payroll-settings, success toast, then GET /payroll/metadata. "Payroll Accounts" and "Payroll Groups" save on their own, outside this button.

- **Locked pay period** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Use a project where a timecard exists. → Open the Payroll tile, change approvers, save.
  - Expected: Banner "Locked. Pay Period was frozen on {date}…"; day pickers locked; PATCH body has no pay_period and no pay_period_locked_at.

- **Time Card tile** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Configure "Time Card Entry Setup". → Pick "Hybrid" and a default model. → Save. → Click the "Department Setup" card.
  - Expected: PUT /payroll/timecards/config with overall_control_model, hybrid_default, approval_cadence; "Department Setup" and "Approval Chain" navigate to /film-tools/timecard/departments and /film-tools/timecard/approvers.

- **Time Card tile clears the Production Controller** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Set a Production Controller in Time Card config. → Save the Time Card tile here. → Re-open Time Card config.
  - Expected: Expected: controller kept. Code: the PUT omits production_controller, so it is most likely cleared.

- **Time Card tile failures are silent** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub GET /payroll/timecards/config 500 and open the tile. → Stub the PUT 500 and save.
  - Expected: Expected: error shown. Code: the load error is ignored (it reads err.status), defaults show; the save failure only logs, no toast, modal stays dirty.

- **Tile load error text** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub GET /purchase-orders/settings 500. → Open the PO tile.
  - Expected: Error pane under "Couldn't load settings" shows the raw server message key, not translated copy.

- **Escape in a stacked picker** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Open the cards tile, change a field. → "Add member" to open the picker. → Press Escape.
  - Expected: Expected: only the picker closes. Code: the setup modal closes too and the edits are lost.

### 02 Setup and configuration › Production Setup / Accounting Setup › Setup tour

- **Tour on an unconfigured project** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub project settings with no companies, banks, currencies, tags, tax types and an empty chart. → Clear localStorage.ah_setup_tour_seen. → As A open /film-tools/account-hub/purchase-orders.
  - Expected: After about 800 ms a modal "Account Hub — Production Setup" with "First we need to set up a few things…" and "Next →"; Next starts bubbles on the sidebar item, in order "Companies & Entities", "Bank Accounts", "Project Currencies", "Account Tags", "Tax Types", "Chart of Accounts" (the last on the Chart of Accounts item).

- **When the tour stays silent** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup · account: non-accountant
  - Steps: Visit /production-setup and /deal-memo on that project. → Log in as non-accountant. → Use a configured project. → Delay the settings GET.
  - Expected: No tour in any of these cases (suppressed paths, accountant-only, nothing missing, not until the doc has loaded).

- **Seen once per project** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Show the tour, press Skip (also try closing the intro, and finishing). → Reload.
  - Expected: localStorage.ah_setup_tour_seen = { "<projectId>": { "production": true } }; the tour doesn't return; clearing the key brings it back.

### 02 Setup and configuration › Production Setup / Accounting Setup › Realtime (two users)

- **Another accountant's changes** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: A and C open Production Setup. → C adds a company, a currency and a bank.
  - Expected: A's Companies card shows the new company without reload (socket production_setup:updated, if the backend emits it). A's currencies and bank cards don't change until A reloads.

### 02 Setup and configuration › Production Setup / Accounting Setup › Errors, empty & loading states

- **Loading states** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Delay GET /account-hub/project-settings 3 s. → Open the full page, then the Settings view.
  - Expected: Full page shows skeleton cards under the header; Settings view shows "Loading Companies…".

- **Settings doc fails to load** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub GET /account-hub/project-settings 500.
  - Expected: Header with "Project-wide configuration. All sections persist to the project_settings row on account-hub-server." and a red box with the raw message (or "Failed to load settings"); no cards.

- **Reference catalogues fail** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Stub preset/isd-codes, preset/currencies, preset/taxes-by-country to fail. → Open the company modal, the currencies card, the tax card.
  - Expected: "Couldn't load countries." / "Couldn't load currencies." / "Couldn't load the tax catalogue." each with "Retry"; Retry refetches; no tax editor while the catalogue is missing.

### 02 Setup and configuration › Production Setup / Accounting Setup › Theme & windowed view

- **Dark theme** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Switch Account Hub to dark. → Open every card, the company, bank and tile modals, and the tour.
  - Expected: All text readable; no white panels; the tour bubble is dark.

- **Windowed view** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/production-setup
  - Steps: Open Production Setup in an MDI window. → Open the bank modal and the quick-create modal.
  - Expected: Modals stay inside the window; the quick-create modal sits above the bank modal and its dropdowns are visible.

### 02 Setup and configuration › Chart of Accounts and Budget (/film-tools/account-hub/chart-of-accounts, /film-tools/account-hub/budget) › Access & navigation

- **Tabs split by cost type** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: On "Cost Accounts" note the rows. → Click "Balance Sheet Codes".
  - Expected: Cost Accounts shows only expense rows; Balance Sheet Codes only asset, liability, capital, income rows; no second GET; "Import Budget" is absent on the Balance tab. Stat strip ("Total codes", Headers, Nominals, Codes, postable) counts the active tab only.

### 02 Setup and configuration › Chart of Accounts and Budget (/film-tools/account-hub/chart-of-accounts, /film-tools/account-hub/budget) › Lists, search, filter, sort

- **Tree order and expand** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Open "Tree" view with codes 2, 1000, 1100.2, 1100.10. → "Collapse all", "Expand all".
  - Expected: Order 2, 1000, 1100.10, 1100.2 (numeric); collapse/expand toggles every branch.

- **Search the tree and table** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Type a child's name in "Search codes or names…". → Switch to "Table". → Type "zzzz".
  - Expected: Tree keeps the match and its ancestors (not its children); table shows matching rows only; no match shows "No accounts match "zzzz"."; footer "filtered from {n}".

- **Table sort** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: "Table" view. → Click Code, Type, Name, Cost type headers twice each.
  - Expected: Each header sorts ascending then descending; "Sorted by {column}" reflects it.

- **Inactive and status chips** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Note "Show inactive" (ON by default). → Turn it OFF. → Hover a budget row.
  - Expected: Inactive rows show the "Inactive" chip and fade; OFF hides them; Non-posting rows show "Non-posting"; budget rows show "BUDGET" on hover; a nameless row shows italic "Unnamed".

- **Empty chart** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Stub the chart GET with [].
  - Expected: "Your Chart of Accounts is empty" with "Import Budget" and "Build from scratch".

### 02 Setup and configuration › Chart of Accounts and Budget (/film-tools/account-hub/chart-of-accounts, /film-tools/account-hub/budget) › Create (bulk add)

- **Add under a parent** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Hover a Group row, click "+" ("Add Header for Group"). → Type a code, wait.
  - Expected: Intro "Adding entries under {parent}…"; POST carries that row's id as parent_id; Codes (sub_category) rows have no "+".

- **Bulk add on Balance Sheet Codes** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: On "Balance Sheet Codes" click "New COA Entry". → Add a code.
  - Expected: Cost type starts as Asset; the saved row appears on the Balance tab, not on Cost Accounts.

- **Grid row controls** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: "Add entry", "Add 5 rows". → Tab off the last row's "Display name". → Shift+Tab.
  - Expected: Rows added; Tab on the last row adds a row and moves the caret into it; Shift+Tab adds nothing.

- **Change a saved code in the grid** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Let a row save. → Change its code.
  - Expected: POST for the new code, then DELETE /account-hub/chart-of-accounts/{oldId} (deactivate). If the DELETE fails, a toast shows and both codes stay active.

- **Remove a saved grid row** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Click "Remove entry" on a saved row.
  - Expected: DELETE /account-hub/chart-of-accounts/{id}; the code is inactive in the chart afterwards.

- **Done flushes pending saves** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Type a code and click "Done" within 2 s. → Stub one create to fail and try again.
  - Expected: "Finishing…", the pending row is saved before the page closes, the chart reloads; a failed row shows "Couldn't save some rows".

- **New code reaches open pickers** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: C has a PO line nominal picker open. → A adds a code in bulk add. → C opens the picker again.
  - Expected: C sees the new code without reload (local ah:coa in the same tab; socket chartofaccounts:updated across users if emitted).

### 02 Setup and configuration › Chart of Accounts and Budget (/film-tools/account-hub/chart-of-accounts, /film-tools/account-hub/budget) › Edit

- **Re-type or re-parent a manual row** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Edit a Nominal with children; change "Line type". → Pick a parent of the wrong type. → Pick a valid parent, save.
  - Expected: Warning "Re-typing this row also re-threads {n} children beneath it."; wrong parent shows "Parent must be a {expected}; "{code}" is a {actual}." and disables Save; valid save sends line_type and parent_id.

- **Budget rows are locked** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Edit a budget-imported row.
  - Expected: Line type, parent and cost type disabled, with "Imported from a budget — its line type is fixed…" and "This row was created by a budget import — it's permanently classified as Expense…"; in Table view its cost type is a read-only chip.

- **Re-classify in Table view** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: "Table" view. → Change a manual row's cost type select to Asset.
  - Expected: PATCH … { cost_type: "asset" }; toast "Cost type updated — {n} descendants also updated" when the server cascades; the row and its sub-tree move to "Balance Sheet Codes".

- **Non-posting codes leave pickers** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Untick "Posting" on a Nominal. → Open a PO line nominal picker.
  - Expected: The code is not offered (chip "Non-posting" in the chart); with only non-posting rows the picker explains why.

- **Modal on a short screen** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Set the viewport to 600 px high. → Open Edit.
  - Expected: The form body scrolls; "Cancel" and "Save changes" stay visible.

### 02 Setup and configuration › Chart of Accounts and Budget (/film-tools/account-hub/chart-of-accounts, /film-tools/account-hub/budget) › Delete / deactivate

- **Reactivate a code** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Edit an inactive code. → Tick "Active", save.
  - Expected: PATCH is_active: true; the "Inactive" chip goes; the code returns to pickers.

- **Server refuses deactivation** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Stub the DELETE with a 422 (code in use / active children). → Confirm.
  - Expected: Error toast with the server message; confirm stays open; row unchanged.

- **Deactivate offered on an inactive row** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: With "Show inactive" ON, hover an inactive row.
  - Expected: Expected: no Deactivate on an already inactive code. Code: the trash shows on every row.

### 02 Setup and configuration › Chart of Accounts and Budget (/film-tools/account-hub/chart-of-accounts, /film-tools/account-hub/budget) › Layers

- **Edit a layer** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Click "Edit set". → Rename, untick Active, "Save".
  - Expected: PATCH /account-hub/tracking-sets/{id}; card updates.

- **Delete a code and a layer** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Delete a code. → Delete a layer that has codes.
  - Expected: Confirms "Delete "LOC-LON"?" and "Delete "{name}" and its {n} codes? This cannot be undone."; DELETE …/nodes/{id} then DELETE /account-hub/tracking-sets/{id}.

- **Delete refused while in use** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Stub the DELETE to fail with a message. → Delete.
  - Expected: Dialog "Can't delete this layer" (or "code") with the server message and "OK".

- **Layer codes in a line picker** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Create an active layer with codes. → Open a PO line "+ Layers" picker.
  - Expected: The picker lists the active layer and its codes (sets fetched with active_only=true).

### 02 Setup and configuration › Chart of Accounts and Budget (/film-tools/account-hub/chart-of-accounts, /film-tools/account-hub/budget) › Budget and import

- **Version detail** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Click a version card.
  - Expected: GET /account-hub/budgets/{id}/lines; tree with Account, Name, Amount, Allocation; container rows show their server rollup_total; unparented lines under "Orphaned ({n})"; LIVE/ARCHIVED cards show a lock; "Total · {name}".

- **Preview and version suggestion** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: After parsing, read the preview. → Click "Back".
  - Expected: Stat tiles Sections, Headers, Nominals, Uncoded, Total; Version = highest project version + 1 ("Next free version in this project (latest: v3)."); Label from the file name; Description "Imported from {file}"; "Append" selected; Back returns to Upload.

- **Duplicate version** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Change Version to an existing one. → "Import" (stub the commit rejection).
  - Expected: Amber "{version} already exists" + "Importing will be rejected unless you change it. Next free: {next}."; Import stays enabled; on rejection: toast and "Import failed" banner, wizard stays on the preview.

- **Import needs a version and label** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Clear Version (then Label).
  - Expected: "Import" disabled while either is blank.

- **Import (Override existing)** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Import a budget with "Override existing". → Open Chart of Accounts.
  - Expected: Commit sends coa_mode: "override"; codes not in the budget show "Inactive"; POs using them still open.

- **Import from Cost Accounts** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: On "Cost Accounts" click "Import Budget" and import.
  - Expected: After the wizard closes the chart reloads with the new codes (and the picker cache is refreshed).

- **Pickers stale after a Budget-page import** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Open a nominal picker (PO line) in the tab. → Import from the Budget page (not Cost Accounts). → Re-open the picker.
  - Expected: Expected: new codes offered. Code: handleCommit only clears the Cost Report cache, so pickers keep the old chart until reload.

- **Parse failure** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Upload the unparseable file (or stub dry-run 422).
  - Expected: Error toast and inline error on the Upload step; no preview.

### 02 Setup and configuration › Chart of Accounts and Budget (/film-tools/account-hub/chart-of-accounts, /film-tools/account-hub/budget) › Errors, empty & loading states

- **Chart fails to load** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Stub the chart GET 500.
  - Expected: "Loading chart of accounts…" then an error toast; empty list with no inline error.

- **Layers fail to load** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Stub GET /account-hub/tracking-sets 500.
  - Expected: "Loading layers…" then an error toast.

### 02 Setup and configuration › Chart of Accounts and Budget (/film-tools/account-hub/chart-of-accounts, /film-tools/account-hub/budget) › Theme & windowed view

- **Dark theme** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Dark mode. → Open tree, table, bulk add grid, Layers, Budget, the wizard.
  - Expected: Dark surfaces and readable text everywhere, grid included.

- **Bulk add in a tool window** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: 02 Setup and configuration
  - Steps: Open Chart of Accounts in an MDI window. → "New COA Entry", open a dropdown in the grid.
  - Expected: The overlay fills that window only; dropdowns render above the overlay.

### 02 Setup and configuration › Approvers › Access & navigation

- **Configured / Not started labels** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Read the label under each module.
  - Expected: Modules with a saved config show "Configured", others "Not started", all from the one summary call.

- **Module deep link** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Open …/approvers?module=card_expenses. → Open ?module=nope.
  - Expected: First selects Production Expense Cards; an unknown value falls back to Purchase Orders.

- **Search modules** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Type "cash" in "Search modules…".
  - Expected: Only Petty Cash Expenses remains.

- **Time Card copy of the screen** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers · account: crew
  - Steps: As A open /film-tools/timecard/approvers, press Back. → As crew with Time Card access open it.
  - Expected: Pinned to Time Card with no module list; Back goes to /film-tools/timecard/config; the crew user sees chains read-only with no "Configure" or "Edit".

### 02 Setup and configuration › Approvers › Default chain (create and edit)

- **Empty default chain** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Select a module with no config.
  - Expected: "Default Approval Levels" card with "No default levels set. Departments without custom configs will have no approval flow." and "Configure".

- **Rules inside a level** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: On a level with a Default rule click "Add more". → Hover a rule, click × ("Remove this rule").
  - Expected: The new rule is preset to "Amount greater than" and locked; × shows only when the level has more than one rule.

- **Insert and remove levels** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Click the dashed "+" ("Insert level here") above, between and below levels. → "Remove level".
  - Expected: Empty levels insert at that spot and renumber on screen; "Remove level" shows only with more than one level.

- **Cancel discards** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Edit a chain, add a user. → "Cancel" (or "Back").
  - Expected: Builder closes with no prompt; nothing sent; the saved chain is unchanged.

### 02 Setup and configuration › Approvers › Approver picker

- **Picker behaviour** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: "Add Users" on a level that already has D in another rule. → Search a name, then a designation.
  - Expected: Title "Add Approvers — Level {n}"; search matches name and designation; admins show "Admin"; D shows "Added" and can't be picked; button "Add {n} users".

- **Rights lookup fails closed** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Stub /v2/access/users to return status: 0. 2. Open the picker.
  - Expected: Only Accounts department members are offered.

- **One user per level, many levels** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Put D on Level 1 and Level → 2. Try to add D to a second rule on Level 1.
  - Expected: D saves on both levels; within Level 1 D shows "Added".

- **Remove an approver chip** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Click × on a user chip ("Remove {name}"). → Save.
  - Expected: User removed from the saved rule.

- **Clearing a rule type drops its users** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: A rule with users; set its type back to "Select a rule...". → Save.
  - Expected: Expected: a warning. Code: chips hide, users still show "Added" in the picker, and they vanish from the saved chain with no warning.

### 02 Setup and configuration › Approvers › Department overrides

- **Department list** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Type in "Search departments…". → Click "All", "Custom", "Default".
  - Expected: Filter counts shown; Custom lists departments with their own config, Default the rest; no match shows "No departments match your search".

- **Custom chain is independent** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Change the default chain after AH-APR-022. → Expand that department.
  - Expected: The department keeps its own chain; the default change doesn't reach it.

- **Emptying an unsaved department** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Edit a department with no config, clear the seeded users, save.
  - Expected: Builder closes; no request.

- **Approver who left the project** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Load a chain containing a removed member (stub).
  - Expected: Builder shows their name (or raw id when unknown); summary counts may exceed the avatars shown.

### 02 Setup and configuration › Approvers › Chain takes effect (two users)

- **Amount rule boundary** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: L1: Default = D, Amount greater than 1000 = C. → Send POs of 999, 1000 and 1001.
  - Expected: Expected by the label: 1000 goes to D. Code (utils/approval-helpers.js compares >=): 1000 goes to C. 999 → D, 1001 → C.

- **Department chain wins** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Department X custom L1 = C; default L1 = D. → Crew in X sends a petty cash float request.
  - Expected: Only C sees it in the queue (no merge with the default chain).

### 02 Setup and configuration › Approvers › Errors, realtime, theme

- **Save failure** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Stub the POST to fail with a server message. → Save.
  - Expected: Red inline banner with the raw server message (or "Failed to save approval levels."); builder stays open.

- **Load failure** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Stub GET …/approval-tiers?module=… 500.
  - Expected: Departments panel shows an error with "Retry"; Retry refetches.

- **Another accountant saves** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: A and C open Purchase Orders. → C saves a chain. → A has the builder open on the same chain and saves.
  - Expected: A's list refreshes silently (socket approval_tier:*); A's open builder doesn't, and A's save overwrites C's (last save wins).

- **Dark theme and windowed view** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/approvers
  - Steps: Dark mode; open the builder and picker. → Repeat in an MDI window.
  - Expected: Readable in dark; picker stays inside the window.

### 02 Setup and configuration › Forms Configuration › Access & navigation

- **Embedded from PO Settings** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Open Purchase Orders › Settings. → Open the Form Configuration card.
  - Expected: Overlay fills the Account Hub content area (not the whole viewport); PO template only; no module rail; no "Set Approver Level".

- **Switch module** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Click "Petty Cash Expenses".
  - Expected: GET …/form-templates?module=cash_expenses; preview shows the float request sections.

### 02 Setup and configuration › Forms Configuration › Edit fields and sections

- **Enter edit mode** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: "Edit Fields". → Click a field.
  - Expected: Tips ("Click a field to edit its properties", "Double-click a section name to rename it", "+ between sections to insert a new section"); property panel with "Field Name", type and "Required"; system fields show "System", custom "Custom".

- **Toggle Required** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config · account: D submit a PO without it
  - Steps: Mark a custom field "Required", save. → As D submit a PO without it.
  - Expected: Template saves required: true; the PO form blocks submission until it's filled.

- **Select field needs a selection type** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Add a field of type "Select". → Open "Selection Type".
  - Expected: Options "Choose selection type...", Vendor, Department, User, Account Code, Tax, Currency, Country, Expenditure Type, Tags; the chosen value saves as selection_type.

- **Hide and restore a system field** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: On a system field click "Remove". → Open "+ Add Custom Field" › "System Fields ({n})". → Restore it.
  - Expected: Removed field disappears (saved hide: true, required: false); it is listed under System Fields; restore brings it back optional.

- **Delete a custom field** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: "Remove" on a custom field ("Delete this custom field"). → Save.
  - Expected: Field is gone from the saved template.

- **Add a section** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Click "+" between two sections. → Name "E2E Extra", press Enter.
  - Expected: Modal "Enter a name for the new section" with "Add Section" disabled until a name; section inserted at that spot with key: "custom_<timestamp>".

- **Rename a section** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Double-click a section name. → Type a new name (then try an empty name).
  - Expected: New name kept; empty name ignored.

- **Delete a section** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Delete a custom section. → Confirm. → Leave without saving.
  - Expected: Confirm "Delete Section" says "…All fields in this section will be removed. This action cannot be undone."; removed locally only; nothing sent until "Save changes".

- **Reorder sections and fields** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: "Rearrange". → Drag a section, then a field. → Save, reload.
  - Expected: New order persists (order renumbered to match).

- **Purpose / Reason stays required** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Petty Cash Expenses › Edit › Float Request › Purpose / Reason.
  - Expected: "Required" toggle on and disabled with "Required by the system. Purpose / Reason can't be made optional."

- **Purpose saved optional reloads required** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: POST a cash template with Purpose required: false (setup). → Reload the page.
  - Expected: Purpose shows as required, and the next save sends required: true.

- **Reset to Defaults** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: With a custom field saved, click "Reset to Defaults".
  - Expected: No confirmation; POST /account-hub/form-templates/reset?module=…; "Template reset to defaults."; custom fields and sections gone.

- **Unsaved edits are dropped** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Make an edit, don't save. → Switch module (or leave the page). → Come back.
  - Expected: Edit lost with no warning.

- **Back from Edit looks saved** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Make an edit. → "Back" out of Edit.
  - Expected: Preview shows the edit as if saved; nothing was sent.

- **Hiding Line Description breaks PO submit** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config · account: D create
  - Steps: Remove the line_description system field, save. → As D create and submit a PO.
  - Expected: Expected: form still submittable or the field protected. Code: POForm validation always demands a line description, so no new PO can be submitted (drafts still save). Restore after.

- **Custom field reaches the forms** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config · account: D create a PO
  - Steps: Add a PO header custom field and a float request custom field; save both. → As D create a PO and a float request.
  - Expected: Both forms show the fields in the saved order; the values save on the PO and the float.

### 02 Setup and configuration › Forms Configuration › Approver levels (second builder)

- **Scope choice** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: "Set Approver Level".
  - Expected: Modal "How would you like to configure approvers?" with "For All Departments" and "For One Department" (then "Choose a department..."); "Continue" disabled until a choice (and a department).

- **Save levels** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: "For All Departments" › Continue. → Add a Default rule and a user. → "Save changes".
  - Expected: Picker lists every accepted member (no rights filter); POST /account-hub/approval-tiers?module=purchase_orders; success banner. The Approvers page shows the same chain.

- **Weaker rules than Approvers** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Save an Amount rule of 0. 2. Save with an empty middle level.
  - Expected: Expected (same as Approvers): blocked / compacted. Code: 0 is accepted and empty levels are saved as they are.

### 02 Setup and configuration › Forms Configuration › Errors, empty & loading states

- **Save failure** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Stub the template POST to fail with a message. → Save.
  - Expected: Red banner with the raw server message (or "Failed to save form template."); edits stay.

- **Load failure** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Stub GET …/form-templates 500.
  - Expected: "Loading form configuration…" then "No form template available for {module}."

- **Reset failure** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Stub the reset POST to fail. → "Reset to Defaults".
  - Expected: "Failed to reset template." (or the raw server message); template unchanged.

### 02 Setup and configuration › Forms Configuration › Realtime and theme

- **Another accountant saves while you edit** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: A in Edit mode with unsaved changes on PO. → C saves the PO template.
  - Expected: A's editor reloads silently (ah:form_template:purchase_orders) and A's unsaved edits are replaced.

- **Dark theme and windowed view** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/form-config
  - Steps: Dark mode; Edit mode and the property panel. → Open in an MDI window and from PO Settings.
  - Expected: Readable in dark; the launcher overlay stays inside the window.

### 02 Setup and configuration › Vendors › Access & navigation

- **Crew Vendors tab** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: As D open the PO tool › Vendors.
  - Expected: Tabs "All", "Verified", "Non-Verified", "Added by Me"; "Create PO" on rows; edit/delete icons only on D's own vendors.

- **Tabs in the URL** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Click "Non-Verified". → Reload.
  - Expected: URL …/vendors/unverified; the tab stays selected after reload; list shows only Non-Verified vendors.

- **Search** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Search by part of a name, a contact, an email, a tax number, a department. → Search "zzzz".
  - Expected: Each matches; no match shows "No vendors match your filter."; count "{n} vendors".

### 02 Setup and configuration › Vendors › Create and validation

- **Crew creates a vendor** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: As D, PO tool › Vendors › "Add Vendor". → Fill and create.
  - Expected: Banner "Vendors created here will be marked Non-Verified."; the vendor shows "Non-Verified" and appears under "Added by Me".

- **Length limits** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Enter 201 chars in name, contact, line 1; 101 in city; 21 in postcode. → Phone "1234". → Paste 60 chars in "Tax Number".
  - Expected: "Max 200 characters" / "Max 100 characters" / "Max 20 characters"; "Phone number must be at least 5 digits"; Tax Number stops at 50.

- **New vendor starts with no country or dial code** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Open "Add Vendor".
  - Expected: "Country" shows "Select country…" and the phone dial code is unset (no UK/+44 preset).

- **Phone is optional** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Create a vendor with no phone. → Create one with dial code +44 and "1753 651700".
  - Expected: First sends phone: null; second sends { country_code: "+44", number: "1753651700" } (non-digits stripped).

- **Postcode autofill** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Country United Kingdom, postcode "SL0 0NH". → Change to a postcode with no match.
  - Expected: City and "State / County" fill from the lookup; a confirmed no-match clears both (City then required).

- **Sort code and account number** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Type "204891" in "Sort Code". → Type "12-34 56A78" in "Account Number".
  - Expected: Sort code shows "20-48-91"; account number keeps digits only, no length cap.

- **Extra bank details** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: "Add additional detail (IBAN, BIC, routing number, etc.)", title "IFSC Code", value "INDB0001234". → Add a row with no title. → Save.
  - Expected: Titled row saved in additional_info with field_type; untitled row dropped; an invalid typed value is only highlighted, not blocked.

- **Accountant-only Default Code, department default** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors · account: A open the form
  - Steps: As A open the form; note "Default Code". → As D open it.
  - Expected: A sees "Default Code"; D doesn't; "Department" preselects the editor's own department.

### 02 Setup and configuration › Vendors › Edit and verify

- **Crew edits only their own vendors** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors · account: D view the list
  - Steps: As D view the list. → Edit D's own vendor, "Save Changes".
  - Expected: Edit/delete icons on D's vendors only; banner "Update the vendor information below. Changes will reset verification to pending."; save sends PATCH (server sets Non-Verified).

- **Edit shows the linked bank record** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Edit a vendor with a bank_id.
  - Expected: GET /api/v2/account-hub/bank-accounts/{bank_id} fills Bank Name, IBAN Code and extras from the bank record.

- **Delete bank details** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Edit a vendor with a linked bank. → Click "Delete" on the Bank Details card, confirm.
  - Expected: Confirm "Delete Bank Details" ("…remove the bank details for "{name}"? This action cannot be undone."); DELETE /api/v2/account-hub/bank-accounts/{bankId} at once (not on Save); bank fields cleared; a 409 (bank in use) shows an error toast.

- **Deep links from Invoices** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Open …/vendors/all?action=add. → Open …?action=edit&id=<id>. → Open …?action=edit&id=unknown.
  - Expected: Add form opens; edit form opens for that vendor and both params are removed; an unknown id does nothing and the params stay.

- **Department fallback overwrites** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors · account: A edit only its phone
  - Steps: Pick a vendor with no department. → As A edit only its phone, save.
  - Expected: Expected: department unchanged. Code: the PATCH sends A's own department (vendor?.departmentId \|\| currentUser?.department_id).

### 02 Setup and configuration › Vendors › Delete

- **Delete refused** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Delete a vendor used on a PO (or stub DELETE 409).
  - Expected: Error toast with the server message; confirm stays open; row stays.

### 02 Setup and configuration › Vendors › History

- **Vendor history** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Click the history icon on a row.
  - Expected: Drawer "Vendor History" loads GET /api/v2/vendors/{id}/history and lists the changes.

### 02 Setup and configuration › Vendors › Vendor pickers and quick-create

- **Retry doesn't duplicate** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Stub the PO create to fail once after the vendor POST succeeds. → Save again.
  - Expected: Only one vendor POST across both saves; the second save reuses the id.

- **Picker rows show status** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Open a PO vendor picker.
  - Expected: Each option shows a monogram, contact line and a "Verified" / "Non-Verified" pill; Non-Verified vendors can still be picked.

- **OCR supplier matching** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: In invoices, open an OCR inbox item whose supplier name matches a vendor in different case. → One with "Ltd" vs "Limited".
  - Expected: Exact (case-insensitive, trimmed) match selects the vendor; the "Ltd/Limited" variant offers a new vendor instead.

- **Send to Vendor needs an email** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Create a PO for a quick-created vendor (no email). → "Send to Vendor". → Repeat with a vendor that has your test email.
  - Expected: Error from the server (po_vendor_email_no_recipient, translated) for the first; the second emails the PO (section 03).

### 02 Setup and configuration › Vendors › Realtime, errors, theme

- **Another user adds a vendor** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: A has the register open. → D creates a vendor.
  - Expected: A's list refetches silently (socket vendor:created → ah:vendor:list).

- **List load failure** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Stub GET /api/v2/vendors 500.
  - Expected: Expected: an error. Code: only console.error; the table says "No vendors match your filter."

- **One malformed row** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Stub the list with one row whose additional_info is the string "{bad".
  - Expected: Expected: other rows still show. Code: an unguarded JSON.parse in mapVendor empties the whole list.

- **Dark theme and windowed view** — planned (e2e/account-hub/02-setup-and-configuration.md)
  - Where: /film-tools/account-hub/vendors
  - Steps: Dark mode; list, form, detail modal, history drawer. → Repeat in an MDI window.
  - Expected: Readable in dark; modals stay inside the window.

### 03 Purchase Orders and Asset Register › Purchase Orders › Access & navigation

- **Crew tab ids are inverted** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U2
  - Steps: As U2, click "Approval Queue". → Click "All POs".
  - Expected: Step 1 URL ends /purchase-orders/all; step 2 URL ends /purchase-orders/all-pos.

- **Hidden console tabs are reachable by URL** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, go to /purchase-orders/settings. → Go to /purchase-orders/posted.
  - Expected: Code today: the Settings panel and the Posted ledger both render (no redirect). Record the result; see Possible defects.

- **PA/FC reload on a gated tab** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: As U6, open "Posted". → Hard-reload the page.
  - Expected: Page stays on the Posted tab with "Posted" visible once the accounts users list loads; no bounce to another tab.

- **Legacy PO URL redirects** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: Log in as U1, then as U5. → Each time go to /film-tools/PurchaseOrder.
  - Expected: Replaced by /film-tools/account-hub/purchase-orders; the role's view renders.

- **Reports and empty PO Entry** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, click "Reports". → Click "PO Entry". → Reload /purchase-orders/po-entry while processing a PO.
  - Expected: Step 1: Coming-soon placeholder titled "Reports", no API call. Steps 2–3: "Select a PO from the Queue tab to begin entry."

- **Approver-only accountant can open but not process** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: Make U5 a tier approver of Dept A. → As U5, click an unassigned Dept A PO in "All POs".
  - Expected: Row is enabled and the detail opens; no "Process" button (not assignee, not PA/FC).

### 03 Purchase Orders and Asset Register › Purchase Orders › Raise / create

- **Typed vendor is created on save** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: In the vendor field type a new name. → Pick Create "<name>". → Submit the PO.
  - Expected: POST /vendors { name } fires right before POST /purchase-orders; the PO references the new vendor id; the vendor appears in Vendors. Delete the vendor after.

- **Vendor quick-create is not duplicated on retry** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub the first POST /purchase-orders to fail. → Submit, then submit again.
  - Expected: Exactly one POST /vendors; the second PO write reuses its id.

- **Create PO from the Vendors tab** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, open "Vendors". → Click a vendor row's "Create PO".
  - Expected: Form opens in "Create PO" mode with that vendor preselected and its address shown under "Vendor Address".

- **Fields that depend on variant / project** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open "Create PO" as U1. → Open "Enter PO" as U5. → Repeat on a TV project.
  - Expected: Crew form has no "Effective Date"; console form has it with min = lock date + 1. "Episode" shows only on TV projects. Line column "Layers" (tracking codes) only in the console form; crew payload has no tracking_codes.

- **Use a saved delivery address** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Under "Delivery address" choose "Use a saved address". → Pick one from "Pick a saved address…". → Submit.
  - Expected: Address fields become read-only; payload has delivery_address_id and save_delivery_address: false; required delivery fields are not validated.

- **Save a new delivery address to the book** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Type a delivery address; enter a postcode + country. → Tick "Save this delivery address for future use". → Submit (not draft).
  - Expected: City and State autofill from the postcode lookup (overwrites); save_delivery_address: true; address appears in "Delivery Addresses" after submit (not after a draft save).

- **Split Line and Split by Period in the form** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Add a line, select it, click "Split Line". → Add a "Rent" line with start/end dates, select it, click "Split by Period" twice.
  - Expected: Step 1: two equal children tagged split, excluded from totals. Step 2: "Split Line" is disabled for the rental; each click appends a full set of period children (cadence from PO Settings). Removing a parent removes its children; the last line cannot be removed.

- **Leaving a dirty form asks first** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Type in the form. → Click the back chip ("Back to POs"). → Cancel, then retry and choose "Leave".
  - Expected: Modal "Unsaved changes" — "You have unsaved changes on this page. Leaving will discard them. Are you sure?"; Cancel keeps the form; "Leave" returns to the list. A browser refresh shows the native leave prompt.

- **Form template fails to load** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub GET /account-hub/form-templates?module=purchase_orders → 500. → Reload and click "Create PO". → Remove the stub, click "Retry".
  - Expected: "Couldn't load the form." with "Retry"; Retry refetches and the PO Details / Delivery Address sections render.

- **Forms Configuration drives fields** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: In PO Settings → Form Configuration hide "Notes", rename "Description", add a custom text field. → Raise a PO filling the custom field. → Open it.
  - Expected: Form: Notes absent, Description uses the new label, custom field present; payload custom_fields: [{ section, fields: [{ name, value }] }]; detail modal shows the renamed label and the custom value. Restore the template after.

### 03 Purchase Orders and Asset Register › Purchase Orders › Edit & drafts

- **Console draft submit skips approval** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, resume a console draft. → Click "Create & Submit PO".
  - Expected: PATCH with status: "ACCT_ENTERED"; row "Acct Entered" in All POs.

- **Update draft vs new draft** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open a draft. → "Save Draft" ▾ → "Save" (Update draft). → "Save Draft" ▾ → "Save As" (New draft).
  - Expected: Step 2 PATCHes the same id; step 3 POSTs a second draft; both listed.

- **Delete a draft** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Click the trash "Delete draft" on a row. → Confirm.
  - Expected: Modal "Delete Draft" — Are you sure you want to delete draft "<name>"? This action cannot be undone.; button "Deleting…" then DELETE /purchase-orders/:id; row gone.

- **Drafts are private** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U1 saves a draft. → U2 opens "PO Drafts".
  - Expected: U2 does not see U1's draft (GET /purchase-orders?status=DRAFT is author-scoped server-side).

- **Create a template** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: "Templates" → "New Template". → Fill fields. → "Save Template", leave the name empty, click save; then type a name.
  - Expected: Crumb "New Template", tag "Template", no "Create & Submit PO" button; empty name shows "Template Name is required"; named save POST /purchase-orders/templates; listed under "Saved Templates".

- **Raise a PO from a template** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Click a template row. → Click "Create & Submit PO".
  - Expected: Form opens with the template's values (crumb "Edit Template"); submit POSTs a new PO; the template itself is unchanged.

- **Update, save-as and delete a template** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open a template → "Save" ▾ → "Save" (Update template). → "Save" ▾ → "Save As" with a new name. → Delete one with the trash icon.
  - Expected: Step 1 PATCH /purchase-orders/templates/:id; step 2 POST (new template); step 3 "Delete Template" confirm → DELETE …/templates/:id.

- **Crew resubmit drops custom fields and attachment** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, edit a pending PO. → Change a custom field and attach a new file. → "Update & Submit PO", reopen it.
  - Expected: Code today: the PATCH has no custom_fields / attachments, so both changes are lost.

- **Edit still offered on a closed PO** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: creator of a closed PO
  - Steps: Log in as the creator of a closed PO. → Open it from "All POs".
  - Expected: Code today: "Edit" shows (closed is treated like pre-approval). Confirm intended; see Possible defects.

- **PO in a locked cost-report period** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: On the locked project, log in as the PO's creator (or U6). → Open a PO whose effective date is on/before the lock.
  - Expected: Banner "Locked period — this purchase order is dated on or before <date> and can't be edited."; no Edit, Process, Return to Approval, Approve/Reject; Delete, History, View PDF, Query and Send to Vendor remain. A console draft whose saved effective date is in the locked period opens with the banner and no Save/Submit.

- **Console edit re-submits as Acct Entered** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5, raise a PO from the tile (crew view, PENDING). → Open it in the console (/purchase-orders/all) → "Edit" → "Update & Submit PO".
  - Expected: Code today: PATCH carries status: "ACCT_ENTERED" and no approvals — the PO skips its pending approval chain. See Possible defects.

### 03 Purchase Orders and Asset Register › Purchase Orders › Validation

- **Required fields come from Forms Configuration** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Mark Vendor, Company, Department, Description, Delivery Date, Notes, Nominal Code and a custom field required. → Submit an empty form.
  - Expected: Red * on each; messages "Vendor is required", "Company is required", "Department is required", "Description is required", "Delivery Date is required", "Notes is required", "Nominal Code is required", "<Field> is required". Unmarked fields are not checked. Restore after.

- **Effective Date required only for accountants** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Mark Effective Date required. → Submit the crew form without it. → Submit the console form without it.
  - Expected: Crew: no error (field not shown). Console: "Effective Date is required".

- **Delivery email and phone** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Type delivery email abc@. → Type phone 1234. → Submit.
  - Expected: "Invalid email format"; "Phone number must be at least 5 digits"; both skipped when a saved address is picked.

- **Rental end before start** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Line Exp. Type "Rent", start 10th, end 5th. → Submit.
  - Expected: "Line 1: Rental End Date cannot be earlier than the Rental Start Date".

- **Quantity × Unit Price overflow** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Quantity 99999999, Unit Price 999999999. → Submit.
  - Expected: "Line 1: Quantity × Unit Price is too large — reduce the quantity or the unit price"; no POST.

- **Required line columns** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Mark Quantity, Unit Price, Exp. Type and a line custom field required. → Line 2 with description, qty 0, no Exp. Type.
  - Expected: "Line 2: Quantity must be > 0", "Line 2: Expenditure Type is required", "Line 2: <field> is required"; a line with no description is skipped; split children are skipped.

### 03 Purchase Orders and Asset Register › Purchase Orders › Submit & approval levels (Default vs "Amount greater than")

- **Amount exactly at the threshold** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, submit a Dept A PO whose gross equals 1000 exactly. → Open it as U3 and as U6.
  - Expected: Code today: resolver uses >=, so the conditional rule matches although the Approvers UI says "Amount greater than" ("> 1000"). Record which user can approve.

- **Department tier with no user for this amount borrows the global tier** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Dept A tier 1 has only a conditional rule (> 1000). → Submit 300.
  - Expected: Tier 1 resolves to the global config's tier-1 users at the same position; they can approve.

- **No approvers configured** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Remove every purchase_orders config on a sandbox project. → Submit a PO.
  - Expected: Pill shows plain "Pending"; nobody gets Approve/Reject; submission was not blocked.

- **Set Approver warning** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Submit a Dept B PO (no tier-1 approver). → Open it in the console as U5. → Click "Set Approver →". → Open it in the crew view.
  - Expected: Console detail shows "No approver configured for this department" and "Set Approver →", which closes the modal and goes to /film-tools/account-hub/approvers. Crew detail never shows it.

- **Auto-approval computed with amount 0** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U3 is in tier-1 Default only. → U3 submits a Dept A PO of 1500 (conditional rule → U6).
  - Expected: Code today: submit resolves the chain with amount 0, so approvals records U3 at tier 1 even though tier 1 at 1500 belongs to U6. Expected per code: pill "Pending (1/2)", U4 (tier 2) is next and U6 is never asked. Correct behaviour would leave tier 1 to U6. See Possible defects.

- **Accountant approver approves from the console** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: Make U5 a tier-1 approver. → As U5 open the pending PO in "All POs" → "Approve".
  - Expected: POST …/approve with the resolved tier_number; modal closes; All POs refetches.

- **Approval Queue badge cleared on action, not on open** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U1 submits. → U3 sees the "Approval Queue" tab and row badge. → U3 opens and closes the PO. → U3 approves.
  - Expected: Badge stays after step 3; cleared after a successful approve (read emitted after the API resolves). A stubbed failing approve leaves it lit.

- **Approver banner and department filter** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: approver for some departments
  - Steps: As an approver for some departments, open "Approval Queue". → Repeat as a global ("all") approver.
  - Expected: Banner "You are assigned as an approver for some departments…" / "…for all departments…"; "Dept:" select lists departments present in the list.

### 03 Purchase Orders and Asset Register › Purchase Orders › Query / reject / resubmit

- **Query visibility and panel** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, open own PO in "My POs" → "Query". → As U6, open any PO → "Query". → As U5 open a PO assigned to U5 but not created by U5. → As U6 (also an approver) open it from a crew Approval Queue.
  - Expected: Steps 1–2: QueryPanel opens and the query badge on the button clears. Step 3: no "Query". Step 4: no "Query" in the Approval Queue tab.

- **Return to Approval only on approved / acct-entered** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open pending, rejected, posted and closed POs as U6. → Open an approved PO in a locked period.
  - Expected: No "Return to Approval" on any of them.

### 03 Purchase Orders and Asset Register › Purchase Orders › Accountant processing & coding

- **Reclaimable tax row** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Give lines a recoverable tax type. → Type an override in the "(Reclaimable tax)" row. → Click "Recalculate from line items". → Save.
  - Expected: Tax row amount = tax of recoverable lines only; override sticks until reset; saved line has is_tax: true; reopening shows one tax row (no duplicate); detail modal and other lists exclude it.

- **Splits on the Process page** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Select a ledger line → "Split Line". → Edit the parent amount. → Select a rental line → "Split by Period".
  - Expected: Children are created; changing the parent rescales its children; split-child Tax cell is not editable; rental splits use the settings cadence.

- **Edit header on the Process page** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Change vendor, company, department, nominal, currency and delivery date under "PO Details". → Save.
  - Expected: PATCH carries vendor_id, company_id, department_id, nominal_code, currency, delivery_date; reload shows them.

- **New ledger line defaults and expressions** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Click "Add line". → Type 2*2 in the amount ("0.00 or 2*2").
  - Expected: New line takes the live header nominal (blank if none, never 2100); amount evaluates to 4.00.

- **PO preview panel** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: On the Process page click "Show PO Preview". → Collapse it with "Collapse PO preview".
  - Expected: Document panel with the PO, "Attachments", and "Approval Chain" listing "Tier n Approval" with approver names, "Pending" for open tiers.

- **Process page in a locked period** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open "Process" on an approved PO dated last month. → In a second session lock the cost-report period past that date. → Watch the open Process page (the lock also reads the live project-settings doc).
  - Expected: Lock banner; Save, Post, Approve/Reject, Return to Approval hidden; effective-date input disabled; History and Delete remain. Code today: "Send to Vendor" stays and its save PATCHes. (Lists hide Process on locked POs, so this page is only reached when the lock moves while it is open.)

- **Full PO fetch fails** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub GET /purchase-orders/:id → 500. → Click "Process".
  - Expected: Code today: page opens on the slim list row with an empty grid. Do not Save (would PATCH line_items: []).

### 03 Purchase Orders and Asset Register › Purchase Orders › Posting & seniority

- **Other post guards** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Clear the effective date, click "Post to Ledger". → Zero every line. → Stub no PO currency and no project default.
  - Expected: Step 1 alert "Enter an effective date before posting to the ledger". Step 2 button reads "Awaiting Balance" and is disabled. Step 3 alert "Select a currency before posting to the ledger".

- **Posted and closed POs reopen for re-coding only** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: As U6, Posted tab → "Process" on an Open row. → Same on a Closed row.
  - Expected: Process page with "Save" but no "Post to Ledger" and no "Assign". Code today: the closed row opens too (the detail modal refuses it).

- **Post line payload differs from Save** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Process a PO with a tax rate shown as "20%" and an uncharted code. → Post (capture the request).
  - Expected: Code today: Post sends tax_rate as typed and account unwrapped, while Save sends 20 and [[code]]. Verify the server accepts both.

- **Assign from the Process page** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: On an approved PO assigned to U5, click "Assign". → Pick U6, reason "Workload Balancing", click "Reassign".
  - Expected: PATCH /purchase-orders/:id { assigned_to: U6, reassignment_reason }; "Assigned to:" shows U6 at once; "Manual assignment · by <name>" and Reason: "<reason>" appear after the PO is reopened (only the assignee is patched locally). Hidden on posted POs.

- **Assign an unassigned PO** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: Log in as U6. → Click "Unassigned" ("Click to assign") on an approved PO and pick U5.
  - Expected: Modal "Assign <PO>" without a reason field; PATCH with reassignment_reason: null; chip shows the name (or "Me").

- **Reassign not offered** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: As U6, look at pending and rejected rows. → As U5, click any assignee chip.
  - Expected: Pending/rejected chips are not clickable; U5's chips are never clickable. Posted and closed stay reassignable for U6.

- **Process-page Assign failure** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub PATCH /purchase-orders/:id → 500. → "Assign" → submit.
  - Expected: Code today: no error toast; modal stays open with no message (only a console error).

### 03 Purchase Orders and Asset Register › Purchase Orders › Send to vendor

- **Uncoded warning** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open a PO with an uncoded line. → Click "Send to Vendor".
  - Expected: Confirm "Line items not coded" with "Send anyway" / "Cancel"; Cancel sends nothing.

- **Resend from the Process page** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Process an already-emailed PO. → Click "Resend to Vendor".
  - Expected: Saves first (PATCH), then confirm "Resend PO to vendor" — "This PO has already been emailed…"; confirm "Resend to vendor" sends a second email.

- **Process-page send stops on mismatch** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Unbalance the ledger. → Click "Send to Vendor".
  - Expected: Alert "Coded lines must match the PO total before you can save"; no PATCH, no email.

- **Server refusals** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub POST …/send-vendor-email → 400 po_vendor_email_no_recipient, click "Send to Vendor". → Repeat with po_vendor_email_failed.
  - Expected: One error toast each with the translated server message; button stays; Process page keeps its data.

### 03 Purchase Orders and Asset Register › Purchase Orders › Close / partial close / cancel / delete

- **Close Off Period closes every visible PO** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Filter Posted by department or quick filter. → Run "Close Off Period".
  - Expected: Code today: all rows sit in one "Unknown" group, so every non-closed unlocked PO in the current view is closed, not one accounting period.

- **Close date in a timezone east of UTC** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Set the browser timezone to Asia/Kolkata. → Close a PO picking the first unlocked day.
  - Expected: Code today: ClosePOModal sends local midnight (previous UTC day) and may fail the server lock check; Close Off Period (UTC) does not.

- **Delete a posted or closed PO** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: creator
  - Steps: As the creator (or U6), open a posted PO. → Click the delete chip and confirm.
  - Expected: Message adds "Its cost report and ledger entries will be removed too."; after delete the PO leaves Posted and All POs.

### 03 Purchase Orders and Asset Register › Purchase Orders › Lists, queues, search, filter, sort, pagination

- **All POs stat cards** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: Log in as U6. → Open "All POs" and apply a quick filter.
  - Expected: Cards "All POs", "Pending", "Approved", "Posted" (counts of the filtered list) and "Total value" converted to the project currency; table columns "PO Number", "Vendor", "Dept", "Amount", "Eff. Date", "Status", "Assigned".

- **Quick filters per tab** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: All POs: click each of "All", "Pending", "Approved", "Rejected", "Posted", "Closed". → Queue and Posted: check their chips.
  - Expected: "Approved" includes Acct Entered rows; Queue chips "All", "Pending", "Approved", "Rejected"; Posted chips "All", "Open", "Fully Relieved", "Partially Relieved", "Closed", "Invoice" (Invoice never matches — see defects). Switching tab resets to "All".

- **Department filter and sort** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Use "Dept:" → "Search department…" to pick a department. → "Sort:" → "Date ↓", "Amount ↓", "Vendor A-Z". → Click column headers twice.
  - Expected: Only that department's rows; sort order changes; header chevrons toggle asc/desc; blank effective dates group together.

- **Console search** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Type part of a PO number, vendor, description, department, nominal, amount (2dp), ISO date or status in "Search all POs — …". → Type text that only appears in a line item.
  - Expected: Matches filter All POs, Queue, Posted, and the Templates / PO Drafts / Delivery Addresses tabs ("No drafts match your search."); line-item text finds nothing (rows carry no lines).

- **Crew lists and empty states** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1, open "My POs", "My Department POs". → As a user with no department, open "My Department POs".
  - Expected: No search bar in the crew view; stat cards "Total POs", "Pending", "Approved", "Total Value"; empty texts "You haven't created any purchase orders yet" / "No purchase orders from your department yet"; only All POs offers "Posted"/"Closed" chips.

- **Bulk selection bar (PA/FC)** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: As U6, tick two rows. → Click another row's body. → Tick the header box.
  - Expected: Bar shows "2 purchase orders", "Total <value>", "Eff. range <date>"; clicking a row toggles it instead of opening; header box selects all unlocked rows; locked rows' checkboxes are disabled with "Locked period — dated on/before <date>". No checkboxes for U5.

- **Bulk Set Eff. Date** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Select rows → "Set Eff. Date". → Pick a date, "Apply".
  - Expected: Modal "Set Effective Date — <n> POs" (min lock + 1, "Apply" disabled until a date); PATCH /purchase-orders/bulk { po_ids, data: { effective_date } }; selection clears; list refetches.

- **Bulk Reassign** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Select pending + approved rows → "Reassign". → Select only pending/rejected rows.
  - Expected: Step 1: only approved rows go in PATCH /purchase-orders/bulk; step 2: button disabled with tooltip "Rejected POs can't be reassigned".

- **Bulk Reassign with mixed assignees** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Select an unassigned approved PO first, then an assigned one. → "Reassign".
  - Expected: Code today: modal acts on the first row ("Assign …", no reason), and reassignment_reason: null is sent for the already-assigned PO too.

- **Bulk cap of 100** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub GET /purchase-orders with 101 rows. → Select all → "Set Eff. Date" → "Apply".
  - Expected: Error toast "Bulk operations are limited to 100 POs at a time"; no request.

- **Queue stat cards** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U5
  - Steps: As U5 open "Queue". → As U6 open "Queue".
  - Expected: Cards "My POs"/"All POs", "Pending", "Ready to process", "Assigned Depts" (U5: departments from rules targeting U5, or "—"; U6: "All (override)"), "My Committed"; U6 also sees "Cash Flow Forecast — Next 6 Weeks".

- **Delivery Addresses tab** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1 open "Delivery Addresses"; click "Edit address" on own and on another user's row. → Clear all fields → "Save Changes". → Fix and save.
  - Expected: Others' rows show a locked edit with "Only the creator or an accountant can edit this" (accountants edit any); "Enter at least a name or address line."; email/phone checks; PATCH /purchase-orders/delivery-addresses/:id; no add or delete here.

### 03 Purchase Orders and Asset Register › Purchase Orders › Attachments & files

- **Attach a file to a PO** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: In the form click "Attach" → "Attachment", pick a PDF. → Use the chip's View / Change / Remove. → Submit.
  - Expected: Chip shows name and size; nothing uploads until submit; on submit the file goes to S3 then attachments: [meta] is in the POST; list rows show the attachment badge. Accepts images, PDF, DOC/DOCX, XLS/XLSX, TXT, CSV (no size check in the UI).

- **Upload failure aborts the save** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub the S3 upload to fail. → Submit with a file.
  - Expected: Chip shows "Failed", error toast; no POST /purchase-orders; form keeps its data.

- **View PDF and download rights** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U1
  - Steps: As U1 open a PO raised by U2 → "View PDF". → Repeat as the creator and as U5.
  - Expected: POST /purchase-orders/:id/pdf, PdfViewer opens; "Download" shown for creator, accountants and users with purchase_order_tool download right only. No Print action.

- **View Attachment** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Open a PO with an image attachment → "View Attachment". → Repeat with a PDF attachment.
  - Expected: Image opens in "Attachment Preview" overlay; PDF opens in PdfViewer.

### 03 Purchase Orders and Asset Register › Purchase Orders › Export / print / download

- **Export selected POs** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: As U6, select rows → "Export" → "Export PDF". → Repeat with "Export Excel".
  - Expected: Label "Exporting…"; POST /purchase-orders/export { ids, format } (blob); file purchase-orders_<timestamp>.pdf / .xlsx downloads. A stubbed 500 shows an error toast and the button recovers.

### 03 Purchase Orders and Asset Register › Purchase Orders › Settings & team rights

- **Settings page** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders · account: U6
  - Steps: Log in as U6. → Open "Settings".
  - Expected: GET /purchase-orders/settings; cards "PO Issuance", "Description Formatting", "Rental & Split Settings", "Asset Register Rules", Form Configuration launcher (crumb "PO Settings / Form Configuration"), "Auto-Assignment Rules"; no "Allow amendments after approval"; no team / posting-rights section.

- **PO number prefix** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Type qa-01xyz99 in "PO number prefix". → Click the card's "Save". → Raise a PO.
  - Expected: Input normalises to QA01XYZ9 (A–Z, 0–9, max 8); PATCH { po_number_prefix, allow_amend_after_approval }; button "Saved"; the new PO number starts with the prefix; old POs keep theirs.

- **Terms and Conditions document** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Upload a 2 MB PDF. → Upload a .png. → Upload a 12 MB PDF.
  - Expected: PDF saves immediately ("Saves on upload") via PATCH { terms_attachment } and shows "Attached" with View/Change; png → "Only PDF, DOC or DOCX files are accepted"; 12 MB → "File must be 10MB or smaller". A failed PATCH rolls the row back.

- **Description and Rental & Split cards** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Pick "ITEM_DDMON" and save. → Turn "Auto-split rental POs" on, "Default Split Type" Weekly, save. → Split a rental line in a new form.
  - Expected: Each card PATCHes only its keys (rental also sends require_effective_date: true, enforce_period_close: true, shown as "Always"); Split by Period now uses weekly windows.

- **Asset Register Rules editor** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Set Expenditure type "Purchase", Price range low 500 high 100. → Fix to 100–500, add a tag, "Save".
  - Expected: "Price low must not exceed price high." blocks Save; saved PATCH { asset_filters }; summary "Lines on a posted or closed PO matching …". Clearing all → "Every line item on a posted or closed PO — no constraint set."

- **Remove a rule** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Click "Remove rule" on a saved rule → confirm. → Add an unsaved rule and remove it.
  - Expected: Confirm "Remove Assignment Rule" — "Are you sure you want to remove this rule?" → DELETE /account-hub/assignment-rules/:id immediately; the unsaved rule disappears without a request.

### 03 Purchase Orders and Asset Register › Purchase Orders › Realtime & badges (two users)

- **New PO reaches accountants live** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U6 keeps "All POs" open. → U1 submits a PO.
  - Expected: U6's list refetches silently (no skeleton) and the "All POs" tab and row show an unread badge; opening the PO clears its row badge.

- **Assignee queue badge** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Complete approval of a Dept A PO. → U5 watches "Queue".
  - Expected: "Queue" tab badge and row badge for U5; opening the PO from Queue clears it.

- **Creator badge on decision** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U3 rejects U1's PO. → U1 opens it from "My POs".
  - Expected: U1's "My POs" tab badge lights; opening the PO in My POs clears it.

- **Drafts tab refreshes** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U1 has "PO Drafts" open in tab A. → Saves a draft in tab B.
  - Expected: Tab A list updates without a loader.

- **Open detail modal is not refreshed** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U4 and U3 both open the same pending PO. → U3 approves tier 1.
  - Expected: Code today: U4's open modal keeps the old approvals (nothing listens to ah:po:detail:<id>); only the list behind refreshes. Record what U4's "Approve" sends.

- **Config changes from another user** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: U5 keeps the module open (All POs, an open form). → U6 edits the tier config, the PO form template and the PO settings.
  - Expected: U5's row gating, open form fields and Settings tab reload silently (ah:approval_tier:list, ah:form_template:purchase_orders, ah:po:settings, ah:assignment_rule:list).

### 03 Purchase Orders and Asset Register › Purchase Orders › Errors, empty & loading states

- **Loading states** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Delay GET /purchase-orders by 3 s. → Open All POs; open a PO.
  - Expected: List skeleton (filter chips, stat cards, table); crew view "Loading purchase orders…"; detail modal skeleton until GET /:id, "Refreshing…" on later loads; "Loading permissions…" overlay while the role resolves.

- **List fetch fails** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub GET /purchase-orders → 500. → Open "All POs" in the console and in the crew view.
  - Expected: Code today: empty table with no error toast.

- **Mutations fail cleanly** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Stub approve, reject, delete, close and Process Save → 500, one at a time. → Run each action.
  - Expected: Exactly one error toast each with the server message; modals stay open (delete confirm, reject modal); Process page keeps edits.

- **Empty states** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Use a project with no POs, templates or addresses. → Open Templates, PO Drafts, Delivery Addresses, Posted and Queue.
  - Expected: "No templates yet" + "Create First Template"; "No drafts yet"; "No delivery addresses yet"; "No posted POs yet."; "No POs in this queue."; crew "No purchase orders found".

### 03 Purchase Orders and Asset Register › Purchase Orders › Theme & windowed view

- **Light and dark** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: Toggle the header theme switch. → Open a detail modal, the reassign modal, View PDF and the Process page.
  - Expected: All surfaces switch together; ReassignModal portal and PdfViewer follow the Account Hub theme.

- **Windowed View** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/account-hub/purchase-orders
  - Steps: In Windowed View open the "Purchase Order" tile as U5. → Open the "Account Hub" tile as U5.
  - Expected: Step 1: crew view (entry=tool read from the window's router). Step 2: console. Both maximized; every flow above works inside the window.

### 03 Purchase Orders and Asset Register › Asset Register › Access & navigation

- **Rights change applies live** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: A3 has the register open. → An admin grants A3 asset_report_tool posting. → Pick a department, then revoke posting.
  - Expected: Picker appears without reload and the list refetches silently; after revoke the picker disappears, the department filter is cleared and the list refetches.

- **Windowed View runs the same module** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: In Windowed View open the "Asset Report" tile. → Repeat AH-AR-006 and AH-AR-016 inside the window.
  - Expected: Window "Asset Report" opens maximized with the "Asset Register" screen (not the legacy PDF report); identical behaviour.

### 03 Purchase Orders and Asset Register › Asset Register › Lists, queues, search, filter, sort, pagination

- **Expenditure type labels** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Open the register. → Read "Exp. Type" on a Purchase, a Rent and a Consumption line.
  - Expected: Purchase → "Purchase", Rent → "Rental", Consumption → "Consumables" (PO screens show "Rent").

- **Department filter** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report · account: A1 pick two departments
  - Steps: As A1 pick two departments in "Department". → Clear them.
  - Expected: Only lines whose PO department is one of them; empty selection = all; names in admin order.

- **Category filter** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Mark one line "Keep" and one "Sell". → Reload. → Click "Keep", "Sell", "All".
  - Expected: Filter shows only matching lines. Code today: after a reload a line only keeps its pill and filter match if /line-items carries category; otherwise it falls out until opened. Record which.

- **Search** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Type part of a description, vendor name, code, PO number or department in "Search assets, vendors, refs…". → Type nonsense.
  - Expected: Matching lines only; nonsense shows "No assets match your filters."; header count and Total follow the filter.

- **Currency conversion** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Pick the extra currency in "Currency". → Pick a selected currency with no rate.
  - Expected: Amounts converted via Production Setup rates (line currency → default → picked); with no rate the face value shows under the new symbol, no warning. With no project currencies selected the picker is empty.

- **Tax lines excluded from totals** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Find a PO's tax line in the list. → Compare the header total and footer "Total" with the visible lines.
  - Expected: Listed, but not added to the header total or footer "Total".

### 03 Purchase Orders and Asset Register › Asset Register › Asset detail (edit, save, attachments)

- **Note-only first save** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Open a line with no asset row, type a note only. → Click the note's "Save".
  - Expected: One POST /asset-register carrying the note (category ""); header Save stays "Saved".

- **Add attachments** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Click "Click to upload" and pick a PNG and a PDF. → Drag-drop an SVG and a 12 MB PDF. → Click header "Save".
  - Expected: Count shows "2 files · 2 pending"; SVG → "<name>: unsupported file type. Only images and PDFs are allowed."; 12 MB → "<name>: exceeds the 10MB per-file limit."; Save uploads the two files first, then PATCH (or POST) with both models.

- **Remove an attachment** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Click × on a saved file. → Save.
  - Expected: PATCH sends the remaining list; file gone after reload (stored file is not deleted).

- **Open an attachment** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Click an image tile. → Click a PDF tile.
  - Expected: Image opens in the overlay viewer with "Download"; PDF opens in PdfViewer with Download for everyone.

- **Unsaved-changes guard** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Change the category, click the back arrow. → Choose "Discard"; repeat and choose "Save & leave". → Change again and press browser Back.
  - Expected: Modal "Unsaved changes" with "Discard" / "Save & leave"; Save & leave writes then closes; browser Back leaves without asking (drafts lost).

- **Existing asset opened through the slim probe** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Give a line a note and an attachment. → Reload; open the line (feed row has no asset_id). → Change the category and Save.
  - Expected: Code today: the editor seeds from the list row, which has no note or files, so they show empty and the PATCH sends attachments: [], wiping the stored files. See Possible defects.

- **Another user's save refreshes the line** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: A1 and A2 open the register. → A2 marks a line "Sell". → A1 has that line open with unsaved edits; A2 saves again.
  - Expected: Step 2: A1's row pill updates silently (no loader or toast). Step 3: A1's open edits are not overwritten; the line refetches on its next open.

- **Load and upload failures** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Stub the S3 upload to return no media, Save with a pending file. → Stub GET /purchase-orders/line-items → 500. → Stub GET /vendors → 500.
  - Expected: Step 1 toast "Upload failed for <name>." (or "Upload configuration unavailable — please retry in a moment."), nothing saved, picks kept. Step 2 error toast and "No assets match your filters.". Step 3 list loads with "—" in Vendor.

### 03 Purchase Orders and Asset Register › Asset Register › Export / print / download

- **Export the register** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report · account: A1 pick a department
  - Steps: As A1 pick a department, category "Keep", a search and another currency. → "Export" → "Export PDF"; then "Export Excel". → Repeat as A3.
  - Expected: POST /purchase-orders/asset-register/export { format, department_ids } (A3: no department_ids); file asset-register_<timestamp>.pdf / .xlsx. Code today: category, search and currency are ignored, so the file can differ from the screen.

### 03 Purchase Orders and Asset Register › Asset Register › Theme & windowed view

- **Register theme toggle** — planned (e2e/account-hub/03-purchase-orders-and-asset-register.md)
  - Where: /film-tools/asset-report
  - Steps: Click the sun/moon ("Toggle colour theme"). → Reload. → Set the Account Hub theme to the opposite and open an image attachment.
  - Expected: Theme flips and persists (localStorage ah:asset-register:theme). Code today: when it disagrees with the Account Hub theme, the image viewer follows the Account Hub theme and the header may go dark.

### 04 Invoices › Invoices / Accounts Payable › Access & navigation

- **Rail links route and highlight** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: As ACC-PA click each rail item from "Overview" to "Settings".
  - Expected: Each opens its path (/overview, /inbox, /register, /matching, /approval, /entry, /payments, /posted, /credits, /creditors, /suppliers, /sales, /accruals, /analytics, /reports, /settings) and that item is highlighted.

- **Fallback and legacy routes** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open /invoices, /invoices/unknown, /invoices/reports, /invoices/cash-close.
  - Expected: First two land on /invoices/overview; Reports shows the Coming Soon page titled "Reports"; cash-close redirects to /film-tools/account-hub/period-close?tab=cash-close.

- **Rail back arrow** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices · account: ACC-PA click the back arrow
  - Steps: As ACC-PA click the back arrow (aria-label "Back to dashboard").
  - Expected: Goes to /film-tools/account-hub/dashboard, a route that is commented out, so the hub catch-all lands on the Purchase Orders default tab. Confirm this is intended.

- **Crew tab lives in ?tab=** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Click "My Invoices". → Reload. → Open ?tab=xyz.
  - Expected: ?tab=my survives reload; an unknown value shows "Approval Queue"; tab clicks replace the URL (Back leaves the page, not the tab).

- **Tool-tile entry uses the PO tool right** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: TOOL-ONLY opens /film-tools/account-hub/invoices?entry=tool. → Admin revokes purchase_order_tool view.
  - Expected: Step 1 stays on the page (no hub right needed). Step 2 redirects to /film-tools.

- **Payment Run Approval tab visibility** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices · account: CREW-RA2
  - Steps: Add CREW-RA2 to Settings › Run Authorization. → Sign in as CREW-RA2 and as CREW-U.
  - Expected: CREW-RA2 sees a right-hand tab "Payment Run Approval"; CREW-U does not.

### 04 Invoices › Invoices / Accounts Payable › Upload & inbox

- **Enter Invoice modal layout** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA › "Invoice Inbox" › "Enter Invoice".
  - Expected: Modal "Enter Invoice" with tabs "Upload Invoice" (selected) and "Manual Entry"; drop zone "Drop invoices here, or click to browse" and "PDF, JPG or PNG · up to 10 files, each 10MB and 5 pages"; "Upload & Submit" disabled with no files.

- **Already paid on upload (accountant only)** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Enter Invoice › Upload, add 2 files. → Switch on "Mark {name} as already paid" for one (the header switch "Mark every file as already paid" does all). → Upload.
  - Expected: Only the flagged file's request carries paid: true; that invoice skips the Inbox and shows "Ready to Pay" in Posted Invoices; the other lands in the Inbox.

- **File list hygiene** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Add the same file twice. → Remove a file with its × (aria "Remove {name}"). → Click "Clear".
  - Expected: A file with the same name and size is listed once; remove and clear work; nothing is sent.

- **More than 10 files** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Add 11 valid PDFs.
  - Expected: "11 files selected — 10 is the most that can go in one upload. Remove 1 and send the rest separately. Nothing has been uploaded."; "Upload & Submit (11)" disabled.

- **Unsupported file type** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Add a .docx with setInputFiles (the picker's accept list hides it). → "Upload & Submit (1)".
  - Expected: Row shows "Only PDF, JPG, or PNG files are allowed"; footer "One file cannot be sent — remove it or replace with a smaller or shorter document. Nothing has been uploaded."; no POST /bulk-upload.

- **Size, page-count and unreadable PDF** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Add the 11 MB PNG, the 6-page PDF and the password-protected PDF. → Submit.
  - Expected: Rows show "File must be 10MB or smaller", "PDF must have 5 pages or fewer", "Could not read PDF — file may be corrupted or password-protected"; footer "3 files cannot be sent — …"; nothing uploaded.

- **Fix a mixed batch** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Add 2 valid files and 1 invalid › submit. → Remove the invalid file › submit again.
  - Expected: First submit sends nothing; second sends exactly 2 files.

- **S3 upload fails for one file** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: page.route the S3 upload to fail for one file. → Upload 2 files.
  - Expected: Counter "Failed to upload" 1 and button "Retry 1" (aria "Retry 1 failed upload"); batch does not auto-clear; Retry starts a new batch and the old Retry disappears; × "Dismiss this upload" removes the finished row.

- **Bulk-upload POST fails** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub POST /api/v2/invoices/bulk-upload → 500. → Upload 1 file.
  - Expected: Counter "Could not be sent"; no Retry button; the POST is not repeated; the row stays until dismissed.

- **Upload continues elsewhere; unload guard** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Start a 5-file upload. → Open "Approval Queue" at once. → Try to close the tab while files are still going up.
  - Expected: Browser leave-page prompt while files are unposted; back on Inbox › "Ongoing Uploads" the batch has moved on or finished.

- **Connection drops mid-upload** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Start an upload; context.setOffline(true). → Go back online.
  - Expected: Row reads "Waiting for the connection — this will carry on by itself."; upload resumes and completes.

- **Ongoing Uploads empty and other sessions** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open "Ongoing Uploads" with nothing running. → ACC-SR uploads from another context.
  - Expected: "No uploads in progress. Files appear here while they upload and extract, then clear themselves."; ACC-SR's batch shows "Uploaded elsewhere" with no Retry or Dismiss.

- **Manual Entry, already paid** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: As 029, set "Paid" to "Already paid". → "Create Invoice".
  - Expected: Request has paid: true and no status; the invoice is not in the Inbox and shows "Ready to Pay" in Posted Invoices.

- **Net and Tax drive Gross** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Type Net 100, Tax 20. 2. Change Gross to 130. 3. "Create Invoice".
  - Expected: Gross fills 120 at step 1; step 3 opens "Amounts don't match" ("Net £100.00 + Tax £20.00 = £120.00, but Gross is £130.00 (£10.00 over). Continue anyway?") with "Create anyway" and "Go back".

- **New vendor from Manual Entry** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Type an unknown vendor name and pick its create option. → Close the modal; check Vendors. → Repeat and "Create Invoice".
  - Expected: No vendor exists after step 2; on Create the vendor POST runs first and the invoice is created with its id.

- **Inbox search and empty states** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Search an existing invoice number. → Search "zzzz". → Stub an empty inbox.
  - Expected: GET …/inbox?search=… filters on the server; "No invoices match your search."; empty inbox "Inbox is empty — upload a PDF or add one manually."

- **Inbox list delete is uploader-only** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-U and ACC-PA each upload one invoice. → ACC-PA hovers both rows. → "Delete" on its own row › confirm.
  - Expected: Hover "Delete" only on ACC-PA's own row; dialog "Delete Invoice Entry" / "Are you sure you want to delete invoice {ref}? This action cannot be undone." → DELETE /api/v2/invoices/:id; row gone.

### 04 Invoices › Invoices / Accounts Payable › Receive & PO matching

- **Vendor pre-fill from OCR** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open the invoice whose OCR supplier is "v1 ". → Open the "Brand New Supplier Ltd" one.
  - Expected: First: Vendor pre-selected as V1. Second: no vendor picked and hint "New vendor “Brand New Supplier Ltd” will be created when you accept." (no fuzzy Ltd/Limited matching).

- **Amounts don't match on accept** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Make Net + Tax ≠ Gross; pick no PO. → Accept; then "Go back"; Accept again › "Continue anyway".
  - Expected: "Amounts don't match" with "Continue anyway" / "Go back"; Go back sends nothing; Continue anyway then shows "No PO Selected".

- **New vendor created on accept** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Type an unknown vendor and choose create. → Accept. → Repeat with the vendor create stubbed to fail.
  - Expected: Vendor is created, then /process carries its id. On failure the server message toasts, the modal stays open and no /process is sent.

- **Bank, company and pay method** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Project with one production bank: open an inbox invoice. → Open "Payment Method".
  - Expected: Bank auto-selected and Company follows the bank's entity (only empty fields are filled); options "BACs", "Faster Payment", "Wire Transfer", "Cheque"; an empty pay method shows "BACs".

- **CR-locked inbox invoice** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Set the CR lock after the invoice's saved effective date. → Open it.
  - Expected: Locked-period banner; every field disabled; no "Accept and Match to PO"; "Query", "Delete" and the PO chip view still work.

- **Accept fails on the server** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub POST /process → 4xx with a message. → Accept with a PO and notes.
  - Expected: Server message toasts; modal stays open with the values; no /match call.

- **Accept from the Register** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Invoice Register" › click an inbox row › accept. → Repeat with /process stubbed to fail.
  - Expected: Same "Invoice Review" modal and /process call; the row becomes Matching. On failure no toast appears (Register's onSubmit has no catch).

- **Selection quirks** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Tick one row › "Process". → With one row ticked, click another row's body.
  - Expected: Step 1 opens that invoice's review (no bulk call). Step 2 toggles the second row's tick instead of opening it.

### 04 Invoices › Invoices / Accounts Payable › Submit & approval levels

- **Overlay send needs a currency** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub the invoice with empty currency. → "Confirm & Send for Approval".
  - Expected: Toast "Currency is required"; no request. (Row and bulk sends skip this check.)

- **Row and bulk Send for Approval** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: As ACC-JR click "Send for Approval" on a no-PO row. → Tick 2 rows › bar "Send for Approval".
  - Expected: One send-to-approval per invoice; buttons show "Sending…" / "Sending..."; selection clears; rows move to the Approval Queue.

- **Approve Selected** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-APR ticks 2 pending rows it can approve. → "Approve Selected".
  - Expected: One approve per invoice, one after the other; selection clears; list refreshes; CR-locked rows can't be ticked.

- **Chase an approver** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-JR (not on the chain) clicks "Chase" on a pending row.
  - Expected: POST /api/v2/invoices/:id/chase; "Chasing…" then "Chased" for 2 s, then "Chase" again (no client limit); not offered on rejected or override rows.

- **No invoice chain configured** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: With no invoice approval config, send an invoice to approval.
  - Expected: Pill "Pending"; nobody sees "Approve"; only "Chase" and, for override users who aren't approvers, "Override" move it.

- **Approve fails on the server** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub POST /approve → 4xx with a message. → Click the row "Approve", then the modal "Approve".
  - Expected: Expected: server error toast. Code: ApprovalPage.approveOne has no catch, so no toast shows and the error goes to the console.

- **CR-locked Approval Queue row** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Lock past an invoice's effective date. → View its row and modal.
  - Expected: No "Approve", "Override", "Override & Pay"; Delete only for creator/assignee/senior; "Chase" and opening stay; select-all skips it; modal shows the lock banner and keeps only History.

### 04 Invoices › Invoices / Accounts Payable › Hold / override

- **Bulk Hold for Query** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Tick 2 matching rows › "Hold for Query". → Pick "Tax Query" › "Confirm Hold".
  - Expected: Modal title "Hold for Query — 2 invoices"; one hold per invoice; both move to the held rows; "On Hold" tile counts them.

- **Hold modal dismissal; held rows not selectable** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open Hold from the overlay; press Escape. → In the list try to tick a held row; click "Select all".
  - Expected: Escape closes only the hold modal (overlay stays); held rows have no checkbox; "Select all" ticks only matching rows.

- **Held invoice in the overlay** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Click a held row.
  - Expected: Overlay shows "Hold Reason" and "Notes:"; no "Hold", "Send for Approval", "Override" or "Confirm Match ▾".

- **Override refused** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub /override → 403 override_permission_required (and separately cannot_override_held_invoice). → Override.
  - Expected: Server message toasts; the row stays.

- **Wire/cheque with no override rights** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-APR (no override) opens a pending wire invoice: row, then modal. → CREW-APR1 opens it from the crew queue.
  - Expected: Accountant row shows "Approve"; both modals have no Approve and no Reject, so a wire/cheque invoice can't be rejected anywhere and crew can't approve it.

- **Pre-approval ignores the CR lock** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Lock past a matching invoice's effective date. → Try Send, Override and Hold on it.
  - Expected: All still offered (Inbox, Approval and Entry freeze such invoices). Confirm intended.

- **Override rights resolution** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Compare ACC-OVR, ACC-PA (no team row) and ACC-JR on Pre-approval. → Stub GET /api/v2/invoices/settings → 500 and reload as ACC-PA.
  - Expected: ACC-OVR and ACC-PA see Override buttons, ACC-JR doesn't; with the failed fetch ACC-PA loses every Override button for the session.

### 04 Invoices › Invoices / Accounts Payable › Query / reject / resubmit

- **Rejected invoice for the uploader** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-U › "My Invoices" › Quick Filters "Rejected". → Open it.
  - Expected: Row shows "Rejected"; the detail has no edit or resubmit action, only Delete for the creator.

- **Query thread across users** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA opens an inbox invoice › "Query" › posts a message. → CREW-U opens it from "My Invoices". → Click "History".
  - Expected: CREW-U's "Query" button shows an unread count, cleared when the panel opens; "History" lists audit entries from GET /api/v2/invoices/:id/history.

- **Query hidden on approval queues** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open the same invoice from the accountant "Approval Queue", the crew "Approval Queue" tab, the Register and crew "My Invoices".
  - Expected: No "Query" on the two approval queues; "Query" on the Register and My Invoices; "History" on all.

- **Reject fails on the server** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub /reject → 4xx. → Reject with a reason from the accountant queue.
  - Expected: Expected: error toast and the button re-enabled. Code: ApprovalPage.rejectOne has no catch, so "Rejecting..." stays and no toast shows.

### 04 Invoices › Invoices / Accounts Payable › Assignment

- **Assign / Reassign from the detail** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open an unassigned Entry invoice › "Assign". → Pick user + reason › "Assign".
  - Expected: Modal "Assign invoice {number}"; PATCH /:id { assigned_to, assignment_reason } then GET /:id; button now reads "Reassign"; list refreshes.

- **Auto-assignment by rule** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Settings › add a rule Departments D1 → ACC-JR › Save. → Override a D1 invoice.
  - Expected: The invoice arrives in Entry assigned to ACC-JR (server rule).

### 04 Invoices › Invoices / Accounts Payable › Line items & coding

- **Open an Entry invoice** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Click an accessible Entry row.
  - Expected: Full-page view with "Invoices / AP / Invoice Entry / {ref} · {vendor}"; header "History", "Query", "Save", "Submit for Review" (non-senior), "Assign"/"Reassign", "Return to Approval", "Post to Ledger" (CAN POST); the Invoices rail hides while the document pane shows; "Back to entry queue" returns.

- **Split a line** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Tick a line › "Split Line" twice. → Edit one child amount. → Edit the parent amount.
  - Expected: First split gives two even children, second three (remainder on the last); editing a child rebalances siblings; editing the parent rescales children keeping their shares; totals count parent lines only.

- **Remove lines; custom tax rate** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Try to remove the only line. → Split, then remove one of the two children. → Pick a predefined tax type, then the custom ("other") type.
  - Expected: The only line can't be removed; removing one of two children collapses the split; the custom rate starts empty.

- **Reclaimable tax line** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Use a recoverable tax type on a line. → Type a different amount on the "(Reclaimable tax)" row. → "Recalculate from line items".
  - Expected: Row amount is computed, then keeps the typed value, then resets; it needs its own nominal before Post.

- **Change currency** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Change "Currency". → "Save".
  - Expected: Warning "Currency changed to {c} — check every line amount is in {c} before posting."; "Invoice Amounts" block and match bar hidden; Save sends net/tax/gross from the lines and reloads the invoice.

### 04 Invoices › Invoices / Accounts Payable › Validation

- **Checklist does not block** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open an invoice with a line missing its nominal.
  - Expected: Header still shows the green "All Validated" pill and the "Data Validation" panel shows passes; only Post's own checks block.

### 04 Invoices › Invoices / Accounts Payable › Posting & seniority / posting limits

- **Senior posts a reviewed invoice** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-SR opens the "Under Review" invoice › "Post to Ledger".
  - Expected: Posts as in 104.

- **Posting limit amount** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-POST (limit 1,000) posts a 5,000 invoice.
  - Expected: The app lets it post (no client amount check). Record the server's answer; the rule and currency need a product decision.

- **Post fails after the save** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub POST /:id/post → 4xx. → Post a complete invoice.
  - Expected: Error toast; view stays; the PATCH has saved the edits; status unchanged.

- **CR-locked Entry invoice** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Lock past an Entry invoice's saved effective date. → Open it and view its row.
  - Expected: Lock banner; no Save, Submit for Review, Assign, Return to Approval or Post; History, Query and Delete (if allowed) stay; the row checkbox is disabled with "Locked period — dated on/before {date}".

- **Quick Entry** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Entry › "Quick Entry". → Fill invoice number and Net only › "Post". → Pick a vendor, Nominal, Tax, Eff. Date › "Post".
  - Expected: "Post" disabled until invoice number and Net; then "Select a vendor before posting to the ledger"; success → POST /api/v2/invoices with status: "ready_to_pay", pay_method: "bacs", then navigates to "Payment Runs". Also works for ACC-JR (no CAN POST check).

- **Posted Invoices correction** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Posted Invoices" › open a row (/posted/:id). → Edit a line › "Save". → Lock its period and reopen.
  - Expected: Cards "Awaiting Payment", "Net Total", "Tax Total"; detail shows "Posted" and only "Save" (no Post, Submit, Assign, Return); Save → PATCH /:id and toast; after the lock it is frozen with no Save.

### 04 Invoices › Invoices / Accounts Payable › Payment runs

- **No authoriser banner** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Turn "Can authorise payment runs" off for every team member. → ACC-SR and ACC-PA open Payment Runs.
  - Expected: ACC-SR sees "No one can authorise payment runs" with the Settings › Team & Posting Rights hint; ACC-PA doesn't.

- **Open Items selection** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open "Open Items". → Untick one invoice; go to group page 2. 3. In a group with over 200 invoices click "Show more invoices".
  - Expected: All start ticked; "{n} Selected" counts across pages minus unticked; a one-method selection shows a direct button ("Create BACs Run", "Mark Paid", or disabled "Print Cheque" titled "Cheque printing isn't available yet"); show-more calls GET …/payment-runs/open-items/group.

- **Mixed methods → Process** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Tick BACS, wire and cheque invoices › "Process".
  - Expected: Modal "Process Selected Invoices", "{n} invoices selected with mixed payment methods"; per method: BACS "Create BACs Run", wire/faster "Mark Paid", cheque "Print Cheque" disabled; the clicked button shows "Processing…" and the modal closes on success.

- **Wire confirmation attachments** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: In "Recently Marked as Paid" click "Attach Confirmation". → "Add Attachment" a PDF. → "View", then "Remove".
  - Expected: Modal "Wire Confirmation — {ref}" ("No attachments yet. Upload a bank confirmation below."); POST /:id/wire-attachments; button becomes "View Attachments (1)"; preview opens; Remove → DELETE /:id/wire-attachments/:media (never /undefined).

- **Only wires and faster payments get Mark Paid** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Cheques" › "Ready to Print". → Open a BACS invoice's detail. → Open a wire invoice's detail.
  - Expected: Cheque and BACS details have no "Mark Paid"; the wire detail has "Mark Paid".

- **Final run level** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-RA2 › "Payment Run Approval" › open the run › "Approve".
  - Expected: Run status becomes approved; its invoices stay "Ready to Pay" (nothing is marked paid). Confirm the expected outcome.

- **Cancel a run** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA opens a run › "Cancel run" › "Keep run". → Again › "Cancel run" in the dialog.
  - Expected: Dialog "Cancel payment run?" / "This will cancel the run and return all invoices back to open items. This action cannot be undone."; Keep run does nothing; confirm → "Cancelling…", DELETE /active-runs/:id; run gone; invoices back in Open Items.

### 04 Invoices › Invoices / Accounts Payable › Creditors

- **Creditors Control** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Creditors Control".
  - Expected: Eyebrow "Creditors", "Outstanding balances and ageing by vendor."; tiles "Total Creditors", "Current", "30 Days", "60+ Days"; "Aged Debt Trend — Last 6 Weeks" with "Estimated"; panel "Vendor Balances" (Vendor, Terms, Current, 30d, 60d+, Total); GET /api/v2/invoices/creditors?page=1&per_page=50&sort=balance_desc.

- **Creditors filters, ageing, sort, search** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: "With Balance Only", then "Overdue". → Ageing "Current Only", "30+ Days", "60+ Days". → Each sort ("Sort: Balance (Low–High)", "Sort: Vendor A–Z", "Sort: Oldest Debt First"). → Search a vendor.
  - Expected: Requests carry filter=with_balance / overdue, ageing=current / 30 / 60, sort=balance_asc / name / oldest, search=; figures exactly as returned.

- **Creditors empty and live refresh** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub an empty response. → In another context post an invoice.
  - Expected: "No vendor balances found."; the page refreshes silently on the invoice event.

### 04 Invoices › Invoices / Accounts Payable › Delete / void

- **Assignee and senior can delete** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-JR (assignee), ACC-SR and ACC-NONE open an Entry invoice created by CREW-U.
  - Expected: ACC-JR and ACC-SR see "Delete"; ACC-NONE does not.

- **Approval Queue delete rules** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: On an untouched pending invoice compare a tier approver, ACC-OVR and ACC-NONE. → After tier 1 signs. → On a CR-locked or overridden invoice.
  - Expected: Untouched: approver and ACC-OVR see hover "Delete", ACC-NONE doesn't. After a signature: ACC-OVR only. Locked / approved / override: no one except creator, assignee or senior.

- **Crew delete** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-U deletes its own invoice from "My Invoices". → CREW-APR1 views an untouched invoice in the "Approval Queue" tab and in "My Department".
  - Expected: Dialog "Delete Invoice" / 'Delete invoice "{name}"? This action cannot be undone.' → DELETE; CREW-APR1 gets Delete only on the Approval Queue tab.

### 04 Invoices › Invoices / Accounts Payable › Lists, search, filter, sort, pagination

- **Server paging** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: With 51+ inbox invoices go to page → 2. Reload. → Tick a row; change page.
  - Expected: URL ?page=2 and request page=2&per_page=50; reload stays on page 2; ticks clear on page change. Same on Pre-approval, Approval Queue, Entry, Register, Posted.

- **Register filters** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Invoice Register": chips "All", "Inbox", "Matching", "Approval", "Entry", "Ready", "Paid". → Department filter; dates "This Week", "This Month", "Last 30 Days", "Last 90 Days".
  - Expected: GET /api/v2/invoices?status=<chip>; "Entry" always returns nothing (no flow sets entry); no chip for held, rejected, override, approved or under_review; department and date filter only the current page.

- **Entry filters and sort** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Chips "All", "Assigned to Me", "Unassigned", "Ready to Post", "Needs Review". → "All pay methods" → "Wire". → "Amount High-Low", "Amount Low-High", "Vendor A-Z". → Search.
  - Expected: Chips, pay method and sort apply to the current page only; search goes to GET /api/v2/invoices/entry?search=.

- **Crew search and quick filters** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Search on each crew tab. → Quick Filters "Pending", "Approved", "Rejected". → A user with no department opens "My Department".
  - Expected: Approval Queue → GET /invoices/approval?search=, My Department → GET /invoices?department_id=…&search=, My Invoices → GET /invoices/my?search=; no-hit text "No invoices match your search."; quick filters filter the page by approval status; no "Ongoing Uploads" search box; no department → My Department silently shows the approval list.

- **Overview possible duplicates** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Overview" › "Possible Duplicates". → "Confirm" one, "Dismiss" another. → Stub confirm → 500.
  - Expected: POST /duplicates/:id/confirm → "CONFIRMED"; POST /duplicates/:id/dismiss → row removed; no success toast, and the failure shows nothing (console only).

### 04 Invoices › Invoices / Accounts Payable › Attachments & preview

- **Detail document panes** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open a PO-linked invoice from the Register. → Click a card under "Linked POs (n)". → Open an image invoice.
  - Expected: Columns "Invoice PDF", "Purchase Order PDF" and details; the card switches the PO pane and opens the read-only PO view above the modal; images render as images; the PDF viewer offers download.

- **Entry document and posted download** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Entry detail › "Fullscreen". → Same from a posted invoice.
  - Expected: Fullscreen viewer opens; on a posted invoice the viewer has no download.

### 04 Invoices › Invoices / Accounts Payable › Export / print / download

- **Register export** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Invoice Register" › "Export" › "Export PDF". → "Export" › "Export Excel". → Stub the export → 500.
  - Expected: "Exporting…"; POST /api/v2/invoices/export { format } → downloads invoice-register_<timestamp>.pdf and .xlsx; failure shows the server error toast.

### 04 Invoices › Invoices / Accounts Payable › Settings & team rights

- **Team table display, edit, remove** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: View members with limits null, 0, 1000. → Remove one › confirm.
  - Expected: Shows "Unlimited", "Submit Only" + "No direct posting", "1,000" + "Up to 1,000", Yes/No, "Senior"; dialog "Remove Team Member" / "Remove {name} from the team? You can add them back later."; saved by PATCH.

- **Alert Preferences** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Turn "Duplicate invoice detection" off and "Daily AP summary email" on. → "Save".
  - Expected: "Save" appears only after a change; PATCH { alerts: [enabled keys] }; button shows "Saved" briefly.

- **Run Authorization levels** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Add a level, "Add Users" (CREW-RA2). → "Insert level here", then remove a level. → "Save".
  - Expected: "Level 1", "Level 2"…; "No users assigned" on an empty level; PATCH { run_authorization: [{ tier, user: [ids] }] }; CREW-RA2 then sees "Payment Run Approval".

- **Auto-Assignment Rules** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: "Add Rule" › set "Assign to", Departments, Vendors, Nominal codes, "Amount min". → "Save". → Delete it › confirm.
  - Expected: New rule POST /api/v2/account-hub/assignment-rules, edits PATCH; dialog "Delete Assignment Rule" → DELETE at once; empty text 'No rules configured. Click "Add Rule" to auto-assign invoices.'; nominal options come from the live chart of accounts.

- **Settings twin in Production Setup** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-JR (non-senior) opens Production Setup › Invoices Entry Setup. → Turns "Is Senior" on for self › Save.
  - Expected: Reachable and saves PATCH /invoices/settings (route checks only isAccountant); record the server's answer — it conflicts with "seniors only".

### 04 Invoices › Invoices / Accounts Payable › Realtime & badges (two users)

- **Crew upload reaches accountants** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA sits on Invoice Inbox. → CREW-U uploads an invoice.
  - Expected: The Inbox refreshes by itself when extraction finishes; the "Invoice Inbox" rail item shows an unread count; opening the row clears it.

- **Accountant Approval Queue badges** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: ACC-PA sends an invoice to approval. → ACC-APR on "Approval Queue" opens and closes it. → Approves it.
  - Expected: Row and rail chips appear without reload; opening does not clear the chip; it clears after Approve succeeds.

- **Crew queue and Payment Runs badges** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-APR1 opens an unread invoice in its "Approval Queue" tab. → ACC-RA opens "Payment Runs" after a run is created.
  - Expected: The crew chip clears on open (unlike the accountant queue); the "Payment Runs" rail chip clears when the page opens; opening a run does not read it.

- **New run reaches crew run approvers** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: CREW-RA2 has the crew module open. → ACC-PA creates a BACS run.
  - Expected: "Payment Run Approval" tab gets a count; the run appears there once it is at CREW-RA2's level.

### 04 Invoices › Invoices / Accounts Payable › Errors, empty & loading states

- **Loading states** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Delay list responses with page.route on Inbox, Approval Queue, Entry and the crew tabs.
  - Expected: Tables keep their header over skeleton rows; crew Payment Run Approval shows "Loading payment runs..."; no flash of empty-state text.

- **Empty states** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub empty lists on each page.
  - Expected: "No invoices awaiting approval.", "No invoices awaiting entry. Approve invoices from the Approval Queue to see them here.", "No invoices found. Process inbox entries to see them here.", "No posted invoices yet.", "No posted invoices awaiting payment.", "No wire transfer invoices found.", "No cheque invoices found.", "No active payment runs.", "No invoices in the matching queue. Process inbox entries to see them here.", crew "You haven't uploaded any invoices yet", "No invoices found in your department", "No invoices awaiting your approval".

- **Vendor list fails** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Stub the vendors list → 500. → Open the Inbox and an OCR invoice.
  - Expected: Vendor names read "Unknown"; the OCR vendor is not pre-filled and no "New vendor … will be created" hint is seeded (no duplicate vendors).

### 04 Invoices › Invoices / Accounts Payable › Theme & windowed view

- **Dark mode** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Switch the app to dark theme. → Visit Inbox, Invoice Review, Entry detail, Payment Runs and Settings.
  - Expected: Rail, tables, pills and modals are readable in dark; no white-on-white or black-on-black text.

- **Tool window** — planned (e2e/account-hub/04-invoices.md)
  - Where: /film-tools/account-hub/invoices
  - Steps: Open Account Hub invoices in an MDI tool window. → Use the rail; open a detail modal.
  - Expected: The same module renders in the window (invoices/* in AccountHubToolHost); modals stack above the window.

### 05 Production Expense Cards › Production Expense Cards › Access & navigation

- **Settings route has no guard** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100 (not senior), type /film-tools/account-hub/card-expenses/settings.
  - Expected: The Settings page renders (only the sidebar link is hidden); any refusal must come from PATCH /settings. Confirm with product whether this is acceptable.

- **Unknown or wrong-tree paths redirect** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: CREW
  - Steps: As CREW, open /card-expenses/settings. → As ACC100, open /card-expenses/extension.
  - Expected: Crew lands on /my-receipts; accountant lands on the Overview index.

- **Crew tabs are visibility only, not access control** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: CREW
  - Steps: As CREW (not approver), open /card-expenses/approval. → Open /card-expenses/pending.
  - Expected: Both pages render (empty states "No cards pending your approval." / "No pending receipts in your departments."), proving the hidden tabs are reachable by URL.

- **Accountant sidebar navigation and back buttons** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Click each sidebar item in turn. → Click the sidebar back button (aria-label "Back to dashboard"). → In the crew view click "Back to film tools".
  - Expected: Each URL matches (/cards, /import, /inbox, /transactions, /pending, /approval, /process, /bulk, /topups, /history, /analytics, /reports, /alerts); active item highlighted; "Reports" shows "Reports — Coming Soon". Step 2 lands on the Account Hub default page (Purchase Orders; the dashboard route is commented out). Step 3 lands on /film-tools.

### 05 Production Expense Cards › Production Expense Cards › Card requests & card holders

- **Crew request validation** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open "Request New Card". → Submit with Proposed Limit empty. → Enter 400, Justification only spaces, submit.
  - Expected: Step 2: toast "Proposed limit must be greater than 0". Step 3: toast "Justification is required". No POST /cards.

- **Request cap on weekly salary basis, and when it cannot be checked** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: crew member
  - Steps: Senior sets Basis "Weekly salary rate", multiplier 2, fallback 500. → As a crew member with an active deal, open the form. → As one with no deal, open it. → Stub the rates so the request currency has no exchange rate; submit above the cap.
  - Expected: With a deal: hint "Maximum … — your weekly rate on your active deal × the project multiplier." (GET /api/v2/deal-memo/deal?status=active). Without: "Maximum £500.00 — project cap for requesters with no active deal.". Step 4: hint "Project cap not applied — no exchange rate for {currency}." and the request is sent.

- **Crew edits and resubmits own request (cap applies)** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On own requested card, click the pencil "Edit card details". → Set the limit above the cap, "Re-submit". → Set a limit under the cap, "Re-submit".
  - Expected: Step 2: red inline error "Proposed limit exceeds the project cap of …", no PATCH. Step 3: PATCH /cards/:id with card_limit, status: "requested", justification, currency; modal closes; detail shows the new limit; status stays "Requested".

- **Crew deletes own request** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On own requested card, click "Delete card request". → Confirm "Delete" in "Delete Card Request?".
  - Expected: DELETE /cards/:id; tile removed; server toast.

- **Edit / delete gates** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: Open an active card as its holder. → As ACC100, open another user's requested card.
  - Expected: Step 1: no Edit, no Delete. Step 2: Review/Edit offered (accountant) but no Delete (not the requester).

- **Crew card search, filter and empty states** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Type a holder name / last four in "Search cards by holder, department, last 4...". → Pick each option in the status select ("All Status", "Active", "In-Progress", "Pending Approval", "Requested", "Rejected"). → Search for nonsense.
  - Expected: Matching tiles only; nonsense shows "No cards found matching your filters."; a user with no cards sees "No cards found. Request a new card to get started.".

- **Crew "In-Progress" filter misses overridden cards** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: holder of an override card
  - Steps: As the holder of an override card, pick "In-Progress" in the status filter.
  - Expected: Expected by users: the card (its badge reads "In-Progress"). Code: the filter value is approved only, so the card disappears.

- **Holder picker hides users who already hold a blocking card** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open "Request New Card". → Search for CREW.
  - Expected: CREW (active card with balance) is not offered; CREW2 is.

- **Accountant request validation** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open the form; leave Card Holder empty. → Pick a holder, leave BS Control Code empty, submit. → With two providers configured, leave Card Provider empty.
  - Expected: "Submit Request" disabled with no holder; toast "BS control code is required"; toast "Please select a card provider".

- **Senior sets up a provider from the form** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: In AH-CARD-025, click "Set up provider". → "Set up card provider": Provider Name + Bank. → "Save provider".
  - Expected: Save disabled until Name and Bank are set; GET /settings then PATCH /settings with the existing list plus the new provider; the new provider is picked in the open form. Clean up: remove it in Settings.

- **Card Register search and status pills** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Type a holder name, designation, provider name, last four and BS code in "Search cards by holder, department, last 4, BS code...". → Click each pill "All", "Active", "Approved", "Override", "Pending Approval", "Requested", "Rejected", "Suspended".
  - Expected: Search matches those fields (never the card's own department id); pills filter by wire status.

- **Card Register export** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Card Register → "Export" → "Export PDF". → Repeat with "Export Excel".
  - Expected: Button shows "Exporting…"; POST /api/v2/card-expenses/cards/export (blob); file card-register_<YYYY-MM-DD_HHMM>.pdf / .xlsx downloads (a copy is kept in File Cabinet). All cards are exported, not just the filtered ones.

### 05 Production Expense Cards › Production Expense Cards › Card review, approval & activation

- **Review validation** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open Edit Card Details. → Clear BS Control Code; submit. → Set limit 0; submit. → With providers configured, clear Card Provider.
  - Expected: Toasts "BS control code is required" and "Proposed limit must be greater than 0"; "Submit for Approval" disabled while no provider is picked.

- **Department with no Level 1 approver** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Send a card for a department with no card_expenses levels. → Open it as an accountant.
  - Expected: Banner "No approval tiers configured for this department" with "Set Approval Level →", which opens /film-tools/account-hub/approvers?module=card_expenses.

- **Only the next approver can act** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: APR2
  - Steps: As APR2, open a card still waiting for Level 1.
  - Expected: No "Approve" / "Reject" on the tile or detail.

- **Return to Approval** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100, open an approved (or override / rejected) card. → "Return to Approval" → confirm "Return to approval?".
  - Expected: POST /cards/:id/return-to-approval; status pending; approvals cleared; user stays on the detail.

- **Activate validation** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open the Activate modal. → Type letters and 15 digits. → For a crew-requested card (no provider) with providers configured, leave Card Provider empty.
  - Expected: Letters are dropped; the Activate button stays disabled until a type and exactly 16 digits are present and, when needed, a provider is picked.

- **Assign a physical card to a digital card** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On a "Digital Active" card, "Assign Physical Card". → Enter 16 digits, "Assign Physical Card".
  - Expected: Modal shows "Current Digital Card" and the warning to dismiss the digital card at the issuer; PATCH /cards/:id body only {last4, card_type: "physical", physical_card_number}; badge "Active".

- **Suspend and reactivate** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On an active card, "Suspend". → "Reactivate". → While suspended, check the holder's Card tab.
  - Expected: POST /cards/:id/suspend then /reactivate; the detail stays open showing "Suspended" then "Active"; while suspended the holder sees "+ Request New Card".

- **Inline BS code correction before the first receipt** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open an active card with no receipts. → Click "Edit BS control code", type a new code, "Save BS control code".
  - Expected: PATCH /cards/:id body only {bs_control_code, updated_by}; status and approvals unchanged.

- **BS code pencil hidden when it is unsafe** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open a card that has a receipt. → Stub GET /cards/:id/receipts 500 and open a card. → Open a rejected card. → Open as crew.
  - Expected: No pencil in any case.

- **Card detail receipts and history** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open an active card with receipts; click a receipt row. → Click "History".
  - Expected: GET /cards/:id/receipts once; row opens "Receipt Details" (with Query, except when opened from the crew Approval Queue); "History" opens "Card History" / "Card ···· 1234". Empty: "No receipts for this card yet."; failure: "Couldn't load receipts" / "Try reopening this card.".

### 05 Production Expense Cards › Production Expense Cards › Statements & transactions

- **Statement currency choice** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: With one provider bank currency, open the import modal. → With providers on two currencies, open it. → Stub banks slow. → Stub no providers and no project currencies.
  - Expected: Step 1: auto-selected. Step 2: "Select currency…", Import disabled until picked. Step 3: "Loading currencies…", Import disabled. Step 4: body has no currency key.

- **Submit with only unmatched rows** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Select only rows with no holder.
  - Expected: Button reads "Submit 0 to Crew Portals" and is still enabled; confirm what the server does with holder-less ids.

- **All Transactions default view and export** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open "All Transactions". → "Export" → "Export PDF".
  - Expected: GET /transactions?from=<a month ago>T00:00:00Z (no to for today); tiles "Total", "New", "In Approval", "Posted", "Value". Export: POST /transactions/export (blob, format only), file card-transactions_<ts>.pdf; active filters are not applied.

- **Filters panel** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Filters". → Pick Statement, Card, Department and a date range; "Done". → Reopen, change, press Escape. → "Reset all".
  - Expected: Done sends statement_id, card_id, department_id, from, to; the Filters badge shows the count; Escape discards the draft; future dates are clamped at Done; "Until today" sends no dates; status chips and search filter the returned rows in the client.

- **"In Approval" tile is always 0** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Have transactions in awaiting_approval. → Read the "In Approval" tile.
  - Expected: Expected: the count. Code counts status in_approval, which no list uses, so the tile reads 0.

- **Delete one transaction** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Row kebab → "Delete transaction". → "Delete" in "Delete Transaction?".
  - Expected: DELETE /transactions/:id; row gone after refetch; a matched receipt returns to the Receipt Inbox unmatched. Approved / posted rows have no kebab item and no checkbox.

- **Bulk delete only visible selected rows** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Select three rows. → Type a search that hides one. → "Delete selected" → confirm.
  - Expected: POST /transactions/bulk-delete with the two visible ids only; the hidden one stays selected.

- **Bulk delete that deletes nothing** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Stub the bulk-delete response {status: 1, data: {deletedTransactions: 0}}. → Delete selected.
  - Expected: Warning "Nothing was deleted — those transactions are no longer available."; selection kept.

### 05 Production Expense Cards › Production Expense Cards › Receipts (upload, match, missing receipts)

- **Batch of receipts** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Add Another Receipt" twice, fill all, "Remove" one. → Submit.
  - Expected: Header "2 receipts · £total · £X left"; button "Submit 2 Receipts"; one batch request with two rows.

- **File size limit** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Attach an 11 MB image.
  - Expected: Toast "File must be 10MB or smaller"; file not attached.

- **Exhausted or missing limit** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Stub the active card with receipts_commit = card_limit. → Stub card_limit 0.
  - Expected: "Upload unavailable" beside a disabled "Upload Receipts"; tooltip "Your card limit is fully committed (…)…" or "No spending limit is set on your card yet…".

- **Urgent and top-up flags** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: assignee
  - Steps: Upload with "Mark as urgent" and "Request top-up" ticked. → As the assignee, open it in Process.
  - Expected: Receipt card shows "Urgent"; Process view shows the "Card Top-Up Decision" panel.

- **Edit amount against headroom** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Edit an unapproved receipt; raise the amount above the available headroom. → Lower it instead.
  - Expected: Red "The entered amount exceeds your available card limit of £X." and Save disabled; lowering saves.

- **Delete a receipt** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Click the bin on an unapproved receipt. → "Delete" in "Delete Receipt?".
  - Expected: DELETE /receipts/:id; card refetched (headroom freed). Approved and Posted receipts show no bin.

- **Crew can delete accountant-sent and in-process rows** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On a "Pending Receipt" statement row (and on an "Under Review" / "Escalated" one) look for the bin.
  - Expected: Code shows the bin on every status except approved / posted. Confirm with product whether a cardholder may delete a statement line or a receipt in senior review, and what the server allows.

- **My Transactions list, filters and detail** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Click each chip "All", "Pending Receipt", "Pending Code", "Awaiting Approval", "Approved", "Queried", "Under Review", "Escalated", "Posted". → Open a receipt.
  - Expected: Unmatched receipts show "Unreconciled" instead of the workflow status; each card has the instruction line (e.g. "In approval queue — awaiting review"); detail "Receipt Details" with spinner "Loading receipt details…", "History", "Query"; "Edit Receipt" only for pending receipt / pending code / awaiting approval.

- **Receipt Inbox sections** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open "Receipt Inbox". → Use search "Search by merchant, amount, card holder..." and the status chips.
  - Expected: Sections "System Matched — Confirm & Attach", "No Match", "Duplicate", "Personal"; each receipt in exactly one; "Action needed" notice counts Match Suggested / Unreconciled.

- **Flag personal spend** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On a linked receipt, kebab → "Flag Personal". → On an unlinked one, same.
  - Expected: Linked: POST /transactions/:transaction_id/flag-personal; unlinked: POST /receipts/:id/flag-personal; row moves to "Personal". No confirmation step.

- **Dismiss flags and re-run match** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Dismiss" on a "Possible Duplicate" chip. → "Dismiss" on a "May be Personal" chip. → "Re-run Match".
  - Expected: POST …/dismiss-duplicate, …/dismiss-personal, POST /receipts/rerun-match (no statementId; button "Matching…").

### 05 Production Expense Cards › Production Expense Cards › Coding & coordinator review

- **Save Draft and the code requirement** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open a receipt; "Submit for Approval" with no code. → Enter a description only; "Save Draft".
  - Expected: Submit disabled without a cost code; Save Draft sends PATCH /receipts/:id and the receipt stays in the queue.

- **Coding Queue scope** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: COORD
  - Steps: As COORD, open the tab. → As an accountant coordinator via the tile. → As a coordinator of a department with no pending receipts.
  - Expected: GET /receipts/coding-queue lists only coordinated departments; empty: "No pending receipts in your departments.".

- **Accountant Pending Coding (read only)** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100, "Pending Coding". → Expand a holder group. → "Filters": Card holder, Age "Overdue (>7d)" / "This week" / "Last week", Date range; "Done"; "Reset all".
  - Expected: GET /receipts/pending-coding; grouped by holder with totals; first group auto-expanded; filters narrow rows; no actions on rows; empty "No pending receipts.".

- **Pending Coding search ignores holder** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Type a holder's name in "Search pending by merchant, holder, amount...".
  - Expected: Expected per placeholder: that holder's rows. Code matches description and amount only, so nothing shows.

- **Coding not required skips the queue** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Turn coordinator coding off. → CREW uploads a receipt.
  - Expected: Receipt goes straight to "Awaiting Approval" (server decides); no Coding Queue entry.

### 05 Production Expense Cards › Production Expense Cards › Submit & approval levels (receipts)

- **Bulk approve and override** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Tick two rows. → "Approve 2" (approver) or "Override 2" (override rights). → "Clear".
  - Expected: Floating bar "2 selected"; POST /approvals/bulk {action, receiptIds}; Clear empties the selection.

- **CR-locked rows cannot be selected** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: With a closed period, view a receipt dated on/before the lock.
  - Expected: Checkbox disabled with title "Locked period — dated on/before {date}"; row click with a selection active does not add it.

- **Row Approve/Override ignores the CR lock** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On the locked row of AH-CARD-089, click "Approve" (or "Override").
  - Expected: Code sends the request anyway (only selection is gated). Confirm the server refuses; if it accepts, the lock is not enforced for single actions.

### 05 Production Expense Cards › Production Expense Cards › Query / reject / resubmit

- **Query thread between accountant and crew** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100, open the receipt in Receipt Inbox → "Query" → send a message. → As the holder, open the receipt in My Transactions. → Click "Query" and reply.
  - Expected: Panel title "Query", entity card_receipt; the holder's Query button shows an unread count, cleared when the panel opens; reply visible to the accountant.

- **Query flag from a deduction rule** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Upload a receipt matching the Need Query rule. → Open it in Process as ACC100. → Open it as SEN.
  - Expected: Red pulsing "Query" button; banner "Query required…as per {rule}."; Post hidden for ACC100, shown for SEN.

### 05 Production Expense Cards › Production Expense Cards › Assignment

- **Assign an unassigned receipt** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: SEN
  - Steps: As SEN, open an unassigned receipt → "Assign". → Assign To + Reason → "Assign".
  - Expected: Modal "Assign Receipt"; button disabled until both are set; POST /assignments/:id/assign {assign_to, assigned_by, reason}; view closes and the queue refreshes.

- **Reassign with a custom reason** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open an assigned receipt → "Reassign". → Pick another accountant, Reason "Other (custom reason)", type text. → "Reassign".
  - Expected: "Currently assigned to {name}"; the current assignee is not in the list; POST /assignments/:id/reassign with the typed reason.

### 05 Production Expense Cards › Production Expense Cards › Posting & seniority / posting limits

- **"Lower than" banner promises a reduction that Post refuses** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Code lines below the receipt amount. → Read the banner, then click "Post to Ledger".
  - Expected: Banner says "Posting will reduce the receipt amount to {total}." but Post is blocked by the mismatch alert. Copy or rule is wrong.

- **Effective date required and bounded** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Clear Effective Date; Post. → Open the date picker with a closed period.
  - Expected: Alert "Enter an effective date before posting to the ledger"; picker min is the day after the lock, max today; default date is never inside the lock.

- **Senior-review flag hides Post for non-seniors** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Upload a receipt above the Senior Review rule (200). → Open it as ACC100 (unlimited limit), then as SEN.
  - Expected: Banner "Senior review required because receipt amount is more than £200.00 as per {rule}."; Post hidden for ACC100, shown for SEN. A deduct rule shows "Auto deduction applied" and its auto line is re-sent unchanged.

- **CR-locked receipt is frozen** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Stub (or create) a receipt whose effective_date is inside the closed period. → Open it in Process.
  - Expected: Banner "Locked period — this receipt is dated on or before {date} and can't be edited."; Save, Assign, Escalate, Submit for Review and Post hidden; fields disabled.

- **Missing metadata fails open for posting** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: ACC0
  - Steps: Stub GET /card-expenses/metadata 500. → As ACC0, open an approved receipt.
  - Expected: posting_limit reads as unlimited, so Post shows; Approval Queue / Coding Queue tabs hidden for crew. Server must refuse the post.

### 05 Production Expense Cards › Production Expense Cards › Bulk process & history

- **Batch post selected receipts** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100, "Bulk Process"; tick two own rows. → Set "Account Code", "Department", "Tax Rate", "Top-Up" (or keep "— Keep each —" / "— Per receipt —"). → "Batch Post 2 items".
  - Expected: Rows assigned to others and CR-locked rows have disabled checkboxes; POST /bulk/process {details: {nominal_code, department_id, episode, tax, topup}, items: [{receipt_id}]}; toast "Posted 2" or "Posted N · M failed"; selection and overrides reset.

- **Bulk row click opens receipts of other accountants** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: As ACC100 with nothing selected, click a row assigned to another accountant.
  - Expected: Code opens the full Process view for it (no assignee-or-senior check, unlike Processing Queue). Confirm and compare with AH-CARD-097.

- **Bulk selection survives the filters** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Tick a row. → Filter "All Merchants" / "All Holders" so it is hidden. → "Batch Post 1 item".
  - Expected: Code posts the hidden row too (selection is not intersected with the visible rows).

- **History search and edit** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "History"; search "Search by merchant, amount, card holder, code…". → Open a posted receipt; change a line description; "Save".
  - Expected: GET /receipts/posted-history; the view shows only "History" and "Save" (no Query, Assign, Escalate, top-up or Post); save-process without a status; empty "No receipts found.".

### 05 Production Expense Cards › Production Expense Cards › Top-ups

- **Card Extension without an active card** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: CREW2
  - Steps: As CREW2, open "Card Extension".
  - Expected: "No active card on this project." and no "Request Top-up". With two active cards a "Select card" picker drives the list.

- **Top-up over the card limit at posting** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Stub the receipt with card_balance above card_limit. → Choose "Top Up by Expense"; post.
  - Expected: Toast "Top-up exceeds card limit. Max allowed: £X"; nothing sent.

- **Partial top-up needs a note** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Partial Top-Up" on a row. → Change the amount; leave Note empty. → Type a note; "Submit Partial Top-Up".
  - Expected: Submit disabled without a note; PATCH /topups/:id/partial {amount, note}; status "Partial".

- **Skip a top-up** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Skip" on a pending row.
  - Expected: "Skipping…" then PATCH /topups/:id/skip; row shows "Skipped".

- **Over-limit explainer** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On a row where card balance + amount > card limit, click "Mark Topped Up".
  - Expected: Modal "Top-up would exceed the card limit" with balance, limit, overflow and "You can apply at most {headroom}…" (or "already at its full limit"); no request sent.

### 05 Production Expense Cards › Production Expense Cards › Fund requests

- **Raise a fund request** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Card Register → "Funds". → "New request": "Pay into" bank, "Fund account", Amount. → "Request funds". → Stub providers with no bank and reopen "New request".
  - Expected: Full-page "Fund requests" with "Back to cards"; bank list only banks bound to a card provider; picking one fills the custodian when unambiguous; button disabled until bank, account and amount > 0; POST /api/v2/card-expenses/fund-requests {bank_id, fund_account, amount}; row "In progress". Step 4: "No card provider has a bank bound yet".

- **Receive or cancel a fund request** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: On an open request, "Mark received". → On another, "Cancel".
  - Expected: PATCH /fund-requests/:id/receive → "Received" (server posts DR bank nominal / CR fund account); PATCH …/cancel → "Cancelled". No confirm and no live update from other users.

### 05 Production Expense Cards › Production Expense Cards › Settings & team rights

- **Card providers** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Settings → "Card Accounts & Custodian" → "Add card provider". → Provider Name, Bank (company fills and locks), Card Custodian Account, Card Float Account from / to. → Save.
  - Expected: The Save button appears only once the section is dirty; PATCH /settings {card_providers}; the card forms list the provider after ah:card:settings. Clean up: "Remove provider", Save.

- **Senior forces unlimited and override** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Edit a member; toggle "Is Senior" on. → "Update".
  - Expected: "Unlimited" and "Can Override" switch on and lock; saved posting_limit: null, can_override: true, is_senior: true; row badge "Senior".

- **Remove a team member** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Click the bin on a member row.
  - Expected: PATCH /settings {team_members} sent at once (no confirm); row disappears. Edit → "Update" saves the same way.

- **Request Cap settings** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Toggle "Cap how much can be requested" on, Basis "Max amount", leave the amount empty; Save. → Enter 1,000; Save.
  - Expected: Error "Enter the maximum amount — a cap of 0 would block every request."; then saved; weekly basis needs a multiplier above 0.

- **Deduction rules and quick codes** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Add Rule"; with Title empty Save is disabled. → Title, Process "Senior review", Min amount 200, Enabled, "Add from Quick Codes" chip; save the section. → "Add Code": Label, Nominal, Keywords "fuel, petrol", VAT; save.
  - Expected: PATCH /settings {deduction_rules} with process_type: "senior_review", threshold_type: "min_amount" (for Senior review / Need query the field reads "Threshold" and is locked to "Min amount"; only "Deduct amount" offers "Deduct By" Percentage / Amount); quick_codes saved with keywords split into an array.

- **Auto-assignment rules** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: "Add Rule": Assign to, Departments, Cost Code / Nominal (live COA leaf codes), Amount Max. → Save. → "Remove rule" on a saved rule.
  - Expected: Create POST /api/v2/account-hub/assignment-rules (module card_expenses) with amount_min holding the "Amount Max" value; Remove calls DELETE immediately, before any Save.

### 05 Production Expense Cards › Production Expense Cards › Realtime & badges (two users)

- **Crew tab chips and per-item dots** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: CREW
  - Steps: ACC100 sends a statement line to CREW (or rejects their receipt). → As CREW, watch the tab bar; open the receipt.
  - Expected: "My Transactions" chip count rises without reload; the receipt card shows a dot; opening it emits the read and the dot clears. Coding Queue chip adds coding_queue + pending_coding and both clear on open.

- **Accountant sidebar chips** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: ACC100
  - Steps: CREW requests a card and uploads a receipt. → As ACC100, watch the sidebar. → Open "Top-Up To Do" and "Smart Alerts".
  - Expected: Chips on "Card Register", "Receipt Inbox", "Approval Queue", "Process Card Expenses", "Top-Up To Do", "Smart Alerts" (red counts); opening a card / receipt clears its row dot; the two pages clear their whole chip on mount.

- **Crew approver badge clears only on action** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses · account: APR1
  - Steps: As APR1, open a pending card and a receipt in the Approval Queue without acting. → Approve one.
  - Expected: Badge unchanged after opening; cleared after approve / reject. Card Extension chip clears on page open.

- **Lists refresh on socket events** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open Card Register (accountant) and the crew Card tab side by side. → APR2 gives final approval; CREW uploads a receipt.
  - Expected: Register tile and crew tile update without reload (ah:card:list); inbox, approval and process lists refresh (ah:card:receipt:list); an open card detail stays open showing the new status.

### 05 Production Expense Cards › Production Expense Cards › Overview, analytics & alerts

- **Overview pending actions** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open Overview with work in each stage. → Click each "Pending Actions" row.
  - Expected: Rows "Awaiting crew coding", "In approval queue", "Ready to process", "Card top-ups to action", "Receipts in inbox", "Card requests awaiting approval" (only counts > 0) open /pending, /approval, /process, /topups, /inbox, /cards; holding-account title "Production Expense Cards Holding Account (9900)". No live refresh until remount.

- **Analytics** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open "Analytics".
  - Expected: GET /analytics/overview (no dates); "Spend by Department", "Spend by Card Holder"; empty states "No department data yet." / "No holder data yet.".

- **Smart Alerts triage** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Filter "Anomaly", "Duplicate Risk", "Velocity", "Merchant", "Resolved". → "Investigate" an active alert. → "Resolve" → note → "Confirm Resolution". → "Dismiss" another.
  - Expected: PATCH /alerts/:id/investigate → "Under Investigation"; /resolve {note} → moves to "Recently Resolved"; /dismiss → "Dismissed"; resolve modal shows the user's name, never a raw id. The note is optional.

- **Failed resolve loses the note** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Stub PATCH /alerts/:id/resolve 500. → Type a note, "Confirm Resolution".
  - Expected: Error toast, but the modal closes and the typed note is cleared.

### 05 Production Expense Cards › Production Expense Cards › Errors, empty & loading states

- **Settings call fails** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Stub GET /card-expenses/settings 500. → Open "Request New Card" as ACC100.
  - Expected: No provider-setup block (card_providers_loaded false); empty provider list with the Settings hint; metadata-driven features still work.

- **Empty and failed lists** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Stub empty lists ([] and {}) for approvals, process queue, posted history, alerts. → Stub GET /cards 500. → Stub GET /receipts/posted-history 500.
  - Expected: Step 1: "No items in the approval queue.", "No receipts in processing queue.", "No receipts found.", "All clear — no active alerts."; no crash on {}. Step 2: toast "Failed to load cards" on Card Register and crew Card. Step 3: server error toast and empty table, not a silent blank.

- **Slow receipt detail** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Delay GET /receipts/:id/detail 3 s. → Open a receipt from My Transactions and close it before the response.
  - Expected: Modal shows "Loading receipt details…" while loading; after closing, the late response does not reopen it.

### 05 Production Expense Cards › Production Expense Cards › Theme & windowed view

- **Dark theme on crew and accountant views** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: In the crew header click the theme toggle. → Open "Upload Receipts", Card detail and the console pages.
  - Expected: Dark backgrounds on the module, the upload form header and its drop zone (both empty and attached states), card detail and sidebar; no white flashes.

- **Windowed view** — planned (e2e/account-hub/05-production-expense-cards.md)
  - Where: /film-tools/account-hub/card-expenses
  - Steps: Open Card Expenses as a tool window from the workspace (tile entry). → Switch tabs; open a card detail.
  - Expected: Window opens maximized; crew view (entry flag read from the window router); navigation stays inside the window; modals render above the window.

### 06 Petty Cash Expenses › Petty Cash Expenses › Access & navigation

- **Switch to Out of Pocket and back** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: As CREW click "Out of Pocket", then "Petty Cash". → Repeat as ACCT_POSTER.
  - Expected: Crew: out-of-pocket/submit with sub-tabs "Submit Receipts", "Receipts History"; back to petty-cash/submit-claim. Accountant: out-of-pocket/overview with "Overview", "Post & Ledger", "Payment Routing"; back to petty-cash/overview.

- **Coordinator loses View Floats while on Active Floats** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: COORD stays on petty-cash/floats. → SENIOR_PA turns View Floats off for that row and saves.
  - Expected: COORD's metadata refetches; "Active Floats" disappears and the page is replaced by the first visible tab (ZL-20812).

### 06 Petty Cash Expenses › Petty Cash Expenses › Floats — request and form fields (Forms Configuration)

- **Crew with no floats** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses · account: CREW2
  - Steps: As CREW2 (no floats) open "Float Request".
  - Expected: Notice "Crew Portal — <name> (<department>)" / "Request a new petty cash float for upcoming work."; empty state "You haven't requested a float yet." with "New Float".

- **Pre-filled requester fields** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses · account: CREW
  - Steps: As CREW open the form.
  - Expected: User field read-only "<name> (<designation>)" with helper "Pre-filled from your Zillit profile"; department read-only; "Currency *" set to the project default and the amount placeholder uses its symbol.

- **Duration type and Days** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: In the duration select (empty option "— Select —") pick "Run of Show". → Pick "Days", enter 5, submit.
  - Expected: Days input appears only for "Days"; payload duration_type:"days", duration:5; with Run of Show duration:null. Side note "Heads up · Run of show = float stays open until production wraps…".

- **Request a second float while one is open** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: With one float awaiting approval reopen "Float Request". → "New Float", submit.
  - Expected: Banner "Need cash for upcoming work? Raise a new float request — you can hold more than one at a time."; both floats under "My Floats" with their own status.

- **Custom fields saved and format-checked** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Add custom email, url, number, phone and select fields in Forms Configuration. → Enter bad values, submit. → Fix them, submit.
  - Expected: 2: "<Field> must be a valid email address" / "…must be a valid URL" / "…must be a number" / "…must be a valid phone number". 3: payload custom_fields:[{section, fields:[{name,label,type,value}]}]; values shown under "Additional Details" in the float detail.

- **Request cap — weekly salary basis** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Basis weekly salary × 2, fallback 300. → Open the form as CREW (active deal), then CREW2 (no deal).
  - Expected: CREW: "Maximum <amt> — your weekly rate on your active deal × the project multiplier." (GET /api/v2/deal-memo/deal?status=active fires). CREW2: "Maximum £300 — project cap for requesters with no active deal."; over-cap errors name the basis.

- **Cap advised, not enforced** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses · account: CREW choose a currency
  - Steps: As ACCT_POSTER pick another crew member. → As CREW choose a currency with no exchange rate.
  - Expected: 1: "The requester's own cap applies — checked on submission." and no browser block. 2: "Project cap not applied — no exchange rate for <CCY>." and submit goes through.

- **Unsaved-changes guard (float form and PC receipts)** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Type in the float form, click "Cancel". → Edit a receipt on Submit Receipts, press browser Back.
  - Expected: Modal "Unsaved changes" / "You have unsaved changes on this page. Leaving will discard them. Are you sure?" with "Leave" and "Cancel"; Cancel keeps the data.

- **Crew float detail** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Under "My Floats" click a float.
  - Expected: Modal "Float <ref>" (GET /float-requests/:id/details): status, Summary (Issued, Spent, Topped Up, Final Balance, Returned), "Posted Batches · n", "Top-Ups · n", "Cash Returns · n", "History"; no "Return to Approval", no BS-code edit.

### 06 Petty Cash Expenses › Petty Cash Expenses › Floats — approval

- **Approver not at the next level** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: APPROVER2 opens a float still at level 1.
  - Expected: No Approve or Reject on the row; the detail modal has no action footer.

- **Override needs both grants; otherwise read-only** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Switch on, user without Can Override. → User with Can Override, switch off. → SENIOR_PA (not in Team & Posting Rights). → ACCT_NOTEAM.
  - Expected: No "Override" on rows or in modals in any case; rows stay visible with no Approve/Reject (read-only queue).

- **Department without an approval level** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: A D2 crew member submits a float. → ACCT_POSTER opens Approval Queue and the row.
  - Expected: Row button "Set Approval Level"; modal banner "No approval tiers configured for this department" with "Set Approval Level →", navigating to /film-tools/account-hub/approvers?module=cash_expenses.

### 06 Petty Cash Expenses › Petty Cash Expenses › Floats — issue and balance (Active Floats)

- **Active floats register** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER → "Active Floats".
  - Expected: GET /float-requests/active-floats?sort=created_at&order=desc; notice "Active Floats"; "Active floats register" with "{n} active · {total} outstanding"; rows: name, company chip, "#ref · Submitted <date> · n days active · <duration>", status pill; "History" button only on Collected / Active / Spending / Spent / Pending Return / Closed / Cancelled rows (drawer "History", subtitle "Float <ref>").

- **Correct the BS code on an unspent float** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER clicks the eye ("View float details") on a Collected float with no receipts. → "Edit BS code", clear it, then type a code, "Save". → Open a float with posted or pending receipts.
  - Expected: Save disabled while blank; PATCH /float-requests/:id {bs_code} only; list refreshes. 3: no edit control. CREW never sees it.

- **Return a float to approval** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER opens an Approved, Ready to Collect or Rejected float detail. → "Return to Approval" → confirm.
  - Expected: Confirm "Return to approval?" / "Float <ref> goes back to Awaiting Approval. Its approvals are cleared and tier 1 approvers are notified again."; POST …/return-to-approval; modal closes; float back in APPROVER1's queue as "Pending 0/n". Button absent from Collected onwards and for crew.

- **Drill into a float's batches** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Click a float row. → Click a batch tile.
  - Expected: "Loading batches…" then "n batches" tiles (#ref, "n receipts · date", amount, status) or "No receipts submitted yet for this float."; batch timeline (Submitted, Coordinator Coding, Accounts Audit, Approval, Post & Ledger, Settlement) and claims; link "Go to Audit Queue →" / "Go to Approval Queue →" / "Go to Post & Ledger →" by status.

- **Crew My Overview** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW with one Collected float opens "My Overview". → Move the float to Spending (submit a batch) and reload. → Click "View more" links and Quick actions.
  - Expected: Latest 5 PC claims, 5 OOP claims, 3 floats (GET /claims/overview/my); links go to Receipts History, OOP Receipts History, Float Request, Submit Receipts. The float card ("Float issued", "Spent / returned", "Remaining balance") shows in step 2 but not step 1 (Collected skipped — see defects).

### 06 Petty Cash Expenses › Petty Cash Expenses › Receipts and out-of-pocket claims

- **Unposted batches reduce what is available** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Leave one batch unposted on the float. → Start a new batch.
  - Expected: Available = balance − pending batches (and capped by issued − receipts_commits when the float carries it, which also shows "Committed" and "Available to spend"); the reduce/reimburse switch uses that figure.

- **Optional "Reimburse to Float"** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Click "Reimburse to Float". → Click "Close the Float". → Click "Close the Float" again.
  - Expected: 1: note "<total> will be reimbursed back to your float balance after this batch is posted."; payload follow_up:"top_up", top_up_amount = batch total. 2: switches to close (one at a time). 3: back to follow_up:null.

- **Optional "Close the Float"** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Select "Close the Float" with and without pending batches.
  - Expected: "Close Float" panel: Current float balance − This batch total − n pending batch(es) = Cash to return; heading "Return <x> cash to the accountant" or "No cash to return — this batch will zero out the float"; when overdrawn "Warning: pending batches exceed the current balance by <x>. You may be overdrawn."; payload follow_up:"close".

- **Several spendable floats** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW holds two Collected floats. → Open Submit Receipts and change "Submitting against".
  - Expected: Options "#<ref> — <amt> left", oldest preselected; switching updates balance, currency symbol and settlement; payload float_request_id and currency follow the choice.

- **Add and remove receipts** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "Add another receipt" twice. → "Remove" one.
  - Expected: Cards "Receipt 1…3"; "Remove" only when more than one; "Batch total" and "n receipts" (receipts with vendor and amount) update.

- **Payroll reimbursement only when enabled** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Settings "Reimburse out-of-pocket claims via Payroll" off; CREW overdraws. → Turn it on, reload. → Pick "Add to Payroll", submit.
  - Expected: Off: no method switch, BACS fields only, payload BACS. On: "Bank Transfer (BACS)" / "Add to Payroll"; payroll shows "<amt> will be added to your next payroll run. No bank details required."; payload PAYROLL, no bank_details. (Crew's GET /settings may return nothing — see open questions.)

- **Optional budget coding at submit** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Open "Budget Coding · Optional — leave blank if unsure". → Pick a Cost Code, Episode (TV projects), Description.
  - Expected: Sent as cost_code, episode, coded_description; blank values sent as null; batch routes as usual.

- **Crew Receipts History** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Open "Receipts History" and select a batch. → Try each quick filter. → Open Out of Pocket "Receipts History".
  - Expected: Titles "My Claims — Petty Cash" / "My Claims — Out of Pocket"; GET /claims/my-batches?expense_type=pc / oop; filters All, With Coordinator, In Audit, Awaiting Approval, Ready to Post (includes override), Rejected, Under Review, Escalated, Queried, Posted; detail shows receipts, "Total", "Query", "History".

### 06 Petty Cash Expenses › Petty Cash Expenses › Validation

- **Days and collect date** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Days = 0, submit. → Type a past collect date, submit.
  - Expected: 1: "Days must be a positive number." 2: picker min is today; typed past date gives "Collect Date cannot be in the past.".

- **Currency falls back to the default** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Clear the float "Currency" picker (X), submit. → Stub the project with no default currency, submit an OOP claim without a currency.
  - Expected: 1: picker re-shows the default, no "Currency is required", payload uses the default (ZL-20733). 2: "Currency is required.".

- **Bank details are not required** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Overdraw (or OOP with BACS) leaving Account Name, Sort Code, Account Number blank; submit.
  - Expected: Submits with empty bank_details; no browser check. Confirm with product.

### 06 Petty Cash Expenses › Petty Cash Expenses › Coordinator review

- **Save Draft** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Code some receipts, "Save Draft". → Reopen.
  - Expected: POST /claims/:id/save-claims without effective_date; codes persist; batch stays in the queue.

- **Split a receipt across nominals** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "Split into lines" on a receipt. → Add a line; set nominals summing to the gross.
  - Expected: First line seeded from the receipt; hint "Coding this receipt across multiple cost codes."; card shows "Coded" only when every line has a nominal and lines total the gross; "Hide split" / "Show split".

- **Dept Overview** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: COORD (home department D1) opens "Dept Overview". → A user in D1 who coordinates D3 opens it.
  - Expected: "<Department> — Financial Overview", stats, "<Department> Floats" (Read-only), "OOP Claims — Your Dept"; data from GET /claims/overview/department?department_id=<viewer's own department>, so step 2 shows D1, not D3.

### 06 Petty Cash Expenses › Petty Cash Expenses › Accountant audit

- **Audit Queue list** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER → "Audit Queue".
  - Expected: GET /claims/audit-queue; notice "Audit Queue"; filters All, Petty Cash, Out of Pocket; "Search batches…"; row PC/OOP chip, "In queue <age>", pill "In audit · <assignee>" or "In audit · Unassigned", "Open".

- **Save Progress** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Change codes and Effective Date, "Save Progress". → Reopen.
  - Expected: POST …/save-claims with effective_date; values persist.

- **Tax line and server deductions** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Code a line with a recoverable tax type. → Open a batch where a Deduct amount rule fired.
  - Expected: Reclaimable-tax row is added and needs a nominal when above 0; the server's auto deduction line shows once and is never sent back in claims[].line_items.

- **Query thread on a batch** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: In an In Audit batch click "Query", send a message. → CREW opens the batch in Receipts History → "Query".
  - Expected: Query panel (entity cash_claim, batch id) shows the thread to both; the other side gets an unread chip on "Query".

### 06 Petty Cash Expenses › Petty Cash Expenses › Submit and approval levels (receipt batches)

- **Receipt batch queue** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: APPROVER1 → Approval Queue → "Receipt Batches & OOP".
  - Expected: GET /claims/approval-queue; "{n} items awaiting approval"; filters All, Petty Cash, Out of Pocket; row PC/OOP chip, "#ref · date · n receipts", quoted notes, amount, "Awaiting Approval"; row Approve and Reject only open the modal.

- **Partial approval selection** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Untick one receipt, "Approve". → Untick all.
  - Expected: 1: claim_ids = ticked ids. 2: "Approve" and "Reject" disabled.

- **Levels resolved by amount** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: D1 rules: up to 100 one level, above 100 two. → Approve a 50 and a 500 batch.
  - Expected: 50 needs one approval; 500 needs two.

### 06 Petty Cash Expenses › Petty Cash Expenses › Query / reject / resubmit

- **Reject selected claims** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Tick 1 of 3 receipts, "Reject", give a reason.
  - Expected: Title "Reject 1 Selected Claim", hint "1 claim will be extracted to a separate rejected batch."; payload claim_ids:[id]; the other 2 stay awaiting approval.

- **Crew cannot resubmit a rejected batch** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW opens the rejected batch in Receipts History and in My Overview.
  - Expected: No edit or resubmit control anywhere, although My Overview says "Some receipts have been queried or rejected. Please review and resubmit.".

- **Crew raises a query** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW selects a batch in Receipts History → "Query", sends text. → The assignee opens it in Audit Queue or Post & Ledger.
  - Expected: Accountant's "Query" button shows an unread count; opening the panel clears only the query chip; the row chip in crew history counts every bucket.

### 06 Petty Cash Expenses › Petty Cash Expenses › Assignment

- **Auto-assigned batch label** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: With an auto-assignment rule, CREW submits. → The assignee opens the batch.
  - Expected: "Assigned to <name> (<designation>) · Auto assigned".

### 06 Petty Cash Expenses › Petty Cash Expenses › Posting and seniority / posting limits / senior sign-off

- **Post & Ledger list** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER → Petty Cash "Post & Ledger"; then Out of Pocket "Post & Ledger".
  - Expected: GET /claims/sign-off-queue?expense_type=pc / oop; notice "Approved batches ready to post to the general ledger."; tiles "Ready to post", "Total to post", "Under review"; rows only Ready to Post, Override, Under review (Escalated hidden); "Review" or a lock.

- **Effective date required** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Clear Effective Date, "Post to Ledger".
  - Expected: "Enter an effective date before posting to the ledger"; picker max today, min the day after any cost-report lock.

- **Review / query flags block a non-senior** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Enable a Senior review (or Need query) deduction rule with a min amount; submit a receipt above it. → Sign-off on: ACCT_POSTER opens it. → Sign-off off: reopen.
  - Expected: 2: no Post; "Submit for Review" and "Escalate". 3: Post available. Seniors post in both.

- **Seniors don't escalate** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Sign-off on; SENIOR_PA and TEAM_SENIOR open a batch.
  - Expected: Only "Post to Ledger"; no "Escalate", no "Submit for Review".

- **Sign-off queue** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: SENIOR_PA → Petty Cash → "Sign-off"; then Out of Pocket → "Sign-off". → Expand a batch.
  - Expected: Notice "Senior Sign-off" / "Senior Sign-off — OOP"; tiles "Awaiting Sign-off" with "{e} escalated · {r} for review · {a} approved", "Total Value", "Escalated"; list holds Escalated, Under Review and Ready to Post batches; expanding loads "Claims & Line Items".

- **Return to Accounts** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Expand an Escalated batch → "← Return to Accounts — further amendments".
  - Expected: POST …/deescalate; row removed; button absent on Under Review and Ready to Post rows. Where the batch lands next is unclear (see open questions).

- **Cost-report period lock** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Lock the cost report through a date. → Open a batch whose effective date falls inside it, in Post & Ledger and in Sign-off.
  - Expected: Period-lock banner; Save, Assign/Reassign, Escalate, Post, Approve & Post and Return hidden; fields disabled; Query and History still work.

### 06 Petty Cash Expenses › Petty Cash Expenses › Top-ups and returns

- **Cash Extension without a float** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW with no Collected or Spending float opens "Cash Extension".
  - Expected: "No active float on this project." / "Top-ups can be requested once you hold an active float."; no Request button.

- **Skip a top-up** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "Skip".
  - Expected: PATCH …/skip straight away (no confirm); "Top-up skipped"; settled as "Skipped".

- **Top-up filters, sort and details** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Use the quick filters and the sort. → Click a settled row.
  - Expected: All / Pending / Completed (incl. partial) / Skipped with counts; Oldest first / Newest first / Largest first on both lists; empty "All caught up"; "Top-Up Details" modal (Holder, Float, Amount, Status, Float balance, Float issued, Created, Updated, Note).

- **Record Return on a Pending Return row** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: On a Pending Return float click "Record Return".
  - Expected: Same modal bound to that float (read-only "<name> · <designation>" and "Float #<n> · Balance <b>").

- **Fund requests** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Active Floats → "Funds". → Amount, "Request funds". → "Mark received" on one; "Cancel" on another; "Duplicate" on a cancelled one. → Clear the custodian in Settings and reopen.
  - Expected: Full page "Petty Cash / Fund requests"; Fund account = custodian (no picker); POST /fund-requests {fund_account, currency, amount} → "In progress"; PATCH …/receive → "Received" (posts to the ledger); PATCH …/cancel → "Cancelled"; Duplicate refills the form. 4: "No float custodian configured" / "Set one in Petty Cash Settings before requesting funds." and "Request funds" disabled.

### 06 Petty Cash Expenses › Petty Cash Expenses › Lists, search, filter, sort

- **Search Active Floats** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Search by holder, reference, BS code, company. → Search nonsense.
  - Expected: Client-side filter on those fields (placeholder "Search floats by holder, reference, BS code, company..."); "No floats match “<q>”." vs "No active floats found." for an empty register.

- **Queue filters and search** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: In Audit Queue, both Approval Queue cards and History use All / Petty Cash / Out of Pocket and search by name or #ref.
  - Expected: Lists narrow client-side; "No batches match this filter." / "No requests match your search."; Coding Queue has the type filter without search.

- **Out of Pocket Overview filters** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER → Out of Pocket → "Overview". → Status Pending / Approved / Paid; routing BACS / Payroll.
  - Expected: GET /claims/overview/out-of-pocket; "All Claims": Pending excludes Ready to Post, Override and Posted; Approved = Ready to Post or Override; Paid = Posted; routing by payment method.

- **Petty Cash Overview action queue** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER (non-senior) → Overview with work in every stage. → Click each action item, "New Float", "View full register".
  - Expected: GET /claims/overview/petty-cash; "{n} batches awaiting accounts audit" → Audit Queue; "…awaiting approver sign-off" → Approval Queue; "…approved — ready to post" → Post & Ledger; "…escalated for senior sign-off" → Sign-off (bounced for a non-senior — defect); "BACS file not yet generated — <amt> waiting" → Payment Routing; "No action items — all clear" when empty.

### 06 Petty Cash Expenses › Petty Cash Expenses › Attachments and preview

- **Receipt preview on the batch page** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: In Audit Queue or Post & Ledger open a batch with an image and a PDF receipt. → Open each.
  - Expected: Image → full-size image overlay; PDF → shared PDF viewer, Download shown for accountants, the claim's owner or users with the Cash tool download right; PDFs served as blob URLs are still detected as PDF.

- **Receipt preview in batch approval** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: APPROVER1 (no download right) opens a batch and its PDF receipt.
  - Expected: Viewer opens without Download; image receipts open in the overlay.

- **Receipt upload failure** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Stub the storage upload to fail, submit receipts.
  - Expected: Error toast and form error (fallback "Failed to submit claim"); no POST /claims.

### 06 Petty Cash Expenses › Petty Cash Expenses › Export / print / download

- **Exports (Petty Cash, Out of Pocket, History)** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: On a Petty Cash tab: "Export" → Floats "Export PDF", then Receipts Register "Export Excel". → On an Out of Pocket tab: "Export" → "Export PDF". → History → "Export" → "Export Excel".
  - Expected: Button shows "Exporting…"; POST /float-requests/export {format:"pdf"} → cash-floats_YYYY-MM-DD_HHMM.pdf; POST /claims/export {format:"xlsx", expenseType:"pc", scope:"all"} → pc-receipts_….xlsx; OOP → oop-receipts_….pdf; History scope:"history" → cash-receipts-history_….xlsx; a copy goes to the Production File Cabinet.

- **Export with nothing to export** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Stub /claims/export to answer with a JSON error envelope.
  - Expected: Busy state resets; no message is shown (defect).

- **Payment Routing buttons** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER → Out of Pocket → "Payment Routing". → "Generate BACS file", then "Download" and "Mark uploaded". → "Preview payroll run".
  - Expected: GET /claims/overview/payment-routing; tiles "BACS Ready", "Payroll Auto-added", "BACS File: Not generated"; Generate flips to "Generated" locally only (no request; resets on reload); Download and Mark uploaded do nothing; Preview opens /film-tools/account-hub/payroll/run.

### 06 Petty Cash Expenses › Petty Cash Expenses › Settings and team rights

- **Settings sections** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: SENIOR_PA → "Settings".
  - Expected: GET /settings; banner "Settings · Float accounts, approval chains, coding rules, deduction policies. Applies to both Petty Cash and Out of Pocket."; sections Float Accounts & Custodian, Team & Posting Rights, Department Coordinator Designations, Approval & Override Settings, Request Cap, Reimbursement Settlement, Deduction & Processing Rules, Quick Codes — Auto-mapping, Auto-Assignment Rules.

- **Remove a team member** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Trash icon on a row. → That user opens a batch.
  - Expected: Removed at once (no confirm); the user loses "Post to Ledger" unless PA/FC.

- **Request Cap section** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Turn on "Cap how much can be requested", Max amount 0, Save. → Valid amount, Save. → Basis "Weekly salary rate", multiplier 0, then 1.5.
  - Expected: 1: "Enter the maximum amount — a cap of 0 would block every request." 2: PATCH /settings {request_cap}. 3: "Enter a multiplier above 0."; decimal multiplier saved as shown.

- **Reimbursement Settlement** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Toggle "Reimburse out-of-pocket claims via Payroll", Save. → Watch a submit form already open in another tab.
  - Expected: PATCH /settings {reimburse_to_payroll:true}; the open form shows or hides "Add to Payroll" without reload.

- **Deduction rules and quick codes** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Edit a System rule. → "Add Rule": no Title → Save disabled; Process "Senior review" (threshold fixed to Min amount); "Deduct amount" (Percentage or Min amount); "Add from Quick Codes"; Enabled. → Add a quick code with keywords "fuel, petrol". → Save both sections.
  - Expected: System rows show "System", no delete, Title and Description disabled; PATCH /settings {deduction_rules}; quick code keywords sent as an array; triggers copied from a quick code don't follow later quick-code edits.

- **Auto-assignment rules** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: "Add Rule" (departments, nominal codes, amount min, assignee, active), Save. → Delete a saved rule. → Stub a 500 on save.
  - Expected: POST /api/v2/account-hub/assignment-rules (module cash_expenses) or PATCH …/:id; delete fires DELETE …/:id at once (no confirm); a failed save shows nothing (defect).

- **Edits from another device** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: SENIOR_PA has unsaved edits in a section. → Another senior saves Settings.
  - Expected: The unsaved form isn't overwritten; with no local edits the page refreshes silently.

### 06 Petty Cash Expenses › Petty Cash Expenses › Cash Recon

- **Recon list** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER → "Cash Recon".
  - Expected: Notice "Cash Reconciliation"; "Reconciliation Periods" table (Period, Status, Opening balance, Physical cash, Variance, Created) or "No reconciliations yet"; pills Draft, Under Review, Signed Off.

### 06 Petty Cash Expenses › Petty Cash Expenses › Realtime and badges (two users)

- **Float approval badge** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW submits a float. → APPROVER1 watches the Approval Queue. → APPROVER1 opens the float, then approves.
  - Expected: Chips on "Approval Queue", the "Float Requests" card and the row appear without reload; opening doesn't clear; approving clears (read sent after success).

- **Crew float badge** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: ACCT_POSTER marks a float Ready to Collect. → CREW watches "Float Request" and the "Petty Cash" tab, then opens the float.
  - Expected: Chips appear live; opening the float clears them (tool cash_expenses_label; a hub accountant on Active Floats clears account_hub_label; a tool-tile accountant counts and clears the crew slice).

- **Batch queue badges** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW submits a batch. → COORD (or ACCT_POSTER without coding) watches Coding Queue / Audit Queue and opens the batch.
  - Expected: Tab and row chips appear; opening clears the receipt bucket only; a query chip stays until the Query panel opens.

- **Top-up badges** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: CREW requests a top-up. → ACCT_POSTER watches "Top-Ups", opens the tab. → ACCT_POSTER completes it; CREW watches "Cash Extension".
  - Expected: "Top-Ups" chip appears and clears on visiting the tab; CREW's row flips live; visiting Cash Extension clears its chip.

- **Role changes apply live** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: SENIOR_PA turns Senior Sign-off Required on, or edits cash approval levels in Approvers. → Other users have the module open.
  - Expected: Their tabs and approval buttons update without reload (cash:settings:updated / approval_tier:* → GET /metadata).

### 06 Petty Cash Expenses › Petty Cash Expenses › Errors, empty and loading states

- **Loading states** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Throttle the network; open Active Floats, Post & Ledger, Approval Queue, Submit Receipts.
  - Expected: Skeleton rows (not empty states) while loading; Submit Receipts shows "Loading…" and never flashes "No active float".

- **Empty queues** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Stub each list to [].
  - Expected: "No batches awaiting coding.", "No batches awaiting audit.", "No float requests awaiting your approval.", "No batches awaiting approval.", "No batches ready to post.", "No batches awaiting sign-off.", "No posted batches yet.", "No top-up requests at the moment.", "No active floats found.".

- **API failures** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Stub {status:0, message} on approve, post, ready-to-collect, record-return, create float. → Stub GET /metadata, float details and crew top-ups to fail.
  - Expected: 1: server message as a toast; float form also shows "Failed to submit request" when no message; return modal "Failed to record cash return."; buttons re-enable; badges stay. 2: crew-only tabs with no error; "Failed to load float details"; "Couldn’t load top-ups." with "Retry".

### 06 Petty Cash Expenses › Petty Cash Expenses › Theme and windowed view

- **Dark mode** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Toggle the theme in the header. → Visit each tab and modal.
  - Expected: Pages switch; light-only surfaces remain: Record Manual Cash Return, Reassign, Escalate and Reject footers, the "Close Float" and "Reimburse to Float" panels on Submit Receipts, Fund requests titles (defect).

- **Tool window and full-page views** — planned (e2e/account-hub/06-petty-cash.md)
  - Where: /film-tools/account-hub/cash-expenses
  - Steps: Open the module as an MDI tool window. → Open a batch from Post & Ledger, Audit Queue, History and Coding Queue; open "Funds" and a recon.
  - Expected: Renders inside the window; full-page views hide the header and tabs; the back arrow / "Back to queue" restores them.

### 07 Deal Memo › Access, tabs & pending members › Access & navigation

- **Accountant whose row lacks view_access** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo
  - Steps: Set ACC's deal_memo_tool view_access false. → Open /film-tools/deal-memo.
  - Expected: Code: AccountHubBootstrap's view gate sends them to /film-tools before the accountant fold applies. Record actual and compare with the wiki ("accountants always view").

- **Hidden tabs bounce to the default** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo · account: CREW
  - Steps: As CREW open /film-tools/deal-memo/approval-queue, then /overview, /notices.
  - Expected: Each lands on /my-deal once rights and metadata have loaded.

- **Refresh on a gated tab does not bounce** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo · account: POST
  - Steps: As POST open /nda and press reload. → As AP1 reload /approval-queue.
  - Expected: Both stay on their tab (no flash redirect to the default tab).

- **Notice template deep link is not closed** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo · account: CREW
  - Steps: As CREW open /film-tools/deal-memo/notice-template.
  - Expected: Code: not in isPostOnlyPath, so the "Notice Template" editor renders. A save (PATCH) result depends on the server.

- **Old Talent Deals link redirects** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo
  - Steps: Open /film-tools/deal-memo/talent-deals.
  - Expected: Replaced by /film-tools/talent-deals (separate Talent Deals tool).

- **Metadata retry reveals the queue** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo
  - Steps: page.route the first GET /api/v2/deal-memo/metadata to 500, let the next return is_approver: true. → Open the module as AP1.
  - Expected: No "Approval Queue" tab at first; it appears about 2 s later without reload. Console shows [dealMemo] getMetadata failed (attempt 1).

### 07 Deal Memo › Access, tabs & pending members › Pending members

- **No project-settings or templates read** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo · account: PEND
  - Steps: As PEND open the module with network capture.
  - Expected: No GET /api/v2/account-hub/project-settings and no GET /api/v2/deal-memo/templates.

- **Approver flag does not open the queue for a pending member** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo
  - Steps: Stub /metadata → is_approver: true for PEND. → Open the module.
  - Expected: Still only "My Deal" and "My NDA".

### 07 Deal Memo › Access, tabs & pending members › Theme & windowed view

- **Tool window keeps the header in view** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo
  - Steps: Open Deal Memo from Film Tools as a tool window. → Open "Create Deal Memo" → Union and scroll the builder.
  - Expected: Window opens maximized. Builder header ("Save as Draft", "Issue Memo") stays visible; only one scrollbar (no outer window scroll).

- **Light and dark theme** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo
  - Steps: Use the header theme toggle. → Visit All Deals, a deal page, the builder, My NDA.
  - Expected: Text and badges stay readable in both themes; the NDA letter stays white paper with dark text in dark mode.

### 07 Deal Memo › Deal Memo Setup › Create gate

- **Group without a setup** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: With Union setups only, click "Create Deal Memo". → Choose "Non-Union".
  - Expected: Menu shows "Union" / "Non-Union". Choosing Non-Union opens the pop-up with "Non-Union setup is pending." and the non-union body text.

- **Several setups: pick and reset** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: With two Non-Union setups, open Non-Union. → Pick one in "Select a setup…". → Edit a section, then "Reset to setup".
  - Expected: Picking hydrates the form; "Reset to setup" restores the setup's values. Hint "Switching reloads rates, allowances and conditions — later edits apply to this deal only."

### 07 Deal Memo › Deal Memo Setup › Setup hub

- **Hub opens on Union, URL carries the group** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: All Deals → "Deal Memo Setup". → Click "Non-Union". → Reload.
  - Expected: /setup-hub shows the Union tab; clicking Non-Union replaces the URL with /setup-hub/non-union; reload keeps Non-Union selected.

- **Cards and search** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Open a group with setups. → Type part of a name in "Search setups…". → Type nonsense.
  - Expected: Cards (newest first) show name, creator and designation, created date, a delete icon, an edit icon and "Create Deal Memo". Search filters by name; no match shows "No setups match “{query}”."

- **Empty group shows the inline form and mints nothing** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: On a group with no setups, open it. → Switch tabs back and forth without typing.
  - Expected: The pane shows the builder inline. No POST /api/v2/deal-memo/templates is sent; no TPL-DRFT-… card appears.

- **Edit opens the setup** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Click the edit icon ("Edit this setup"). → Click Back.
  - Expected: /setup-hub/<group>/:id with title "Edit Deal Setup"; Back returns to the group list.

- **Delete a setup** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Click the delete icon. → In "Delete setup?" read the body. → Click "Yes".
  - Expected: Body: "“{name}” will be permanently removed. Deals already created from it are not affected. Are you sure you want to delete this?" DELETE /templates/:id; the card disappears without reload; deals made from it still open.

### 07 Deal Memo › Deal Memo Setup › Setup builder

- **New Union setup layout** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Union tab → "New setup".
  - Expected: /setup-hub/union/new, title "New Deal Setup". Sections: "Territory & Union", then the "Global Deal Memo Setup" band (Production Schedule, Allowances & Rentals, Conditions & Documents, Payroll Bureau) with its note. No "Non-Union Pay Rules".

- **Save Setup writes the project sections** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: On a project with empty global sections, fill Production Schedule dates, one allowance, one condition, one bureau. → Save Setup. → Save a second setup that changes only the bureau.
  - Expected: First save: toast "Deal Memo Setup started from this template — {sections} saved." and the project settings now hold those values. Second: "Deal Memo Setup updated — Payroll Bureau." (or the server's equivalent). A save with nothing different writes nothing.

- **Update an existing setup** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Open a saved setup. → Change the agreement. → Click "Update Setup".
  - Expected: "Update Setup" appears only after a change. PATCH /templates/:id; reopening shows the new agreement.

- **Non-Union Pay Rules save to the project immediately** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Non-Union setup → "Non-Union Pay Rules" → "Edit rules". → Add an overtime row and "Save 1 rule". → Leave without Save Setup; open another Non-Union setup.
  - Expected: PATCH /api/v2/account-hub/project-settings/non-union-paybreakdown fires on the grid save (whole rule book: overtimes, premiums, penalties); the editor closes. The other setup's "Pay rules" preview shows the same new row (one project rule book).

- **Agreement documents upload** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Conditions & Documents → "+ Add Document (PDF)". → Pick a PDF, fill "Title *". → "Save all 1". → Repeat with a .docx and with a 25 MB PDF.
  - Expected: PDF uploads and is added to the project list. The .docx is refused with "{name}: PDF only"; the large file with "{name}: over 20 MB".

- **Standard deal conditions reach new deals** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: In a setup "+ Add Condition", type a clause, Save Setup. → Create a new deal from that group.
  - Expected: The clause is pre-filled in the deal's Conditions & Documents → Conditions.

- **Schedule errors do not block Save Setup** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: In Production Schedule set a phase end before its start. → Save Setup.
  - Expected: Error shows under the phase row, but the setup and the schedule still save.

- **Leave an unsaved new setup** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: New setup, edit, wait for the autosave. → Back. → "Leave Without Saving".
  - Expected: Prompt "Save this setup?" with "You have unsaved changes to this setup. Name and save it before leaving? Leaving without saving discards the draft." and "Keep Editing" / "Leave Without Saving" / "Save Changes". Leave deletes the autosaved template (DELETE /templates/:id) and returns to the group; no card remains.

### 07 Deal Memo › Deal Memo Setup › Rules editors (pay-rules grid, day types, agreement import)

- **Open the pay-rules grid** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Non-Union setup on a project with no rules: look at "Pay rules". → Click "Add overtime, premium & penalty rules". → Back; with rules saved, click a preview row.
  - Expected: Empty preview offers "Add rules" and "Add overtime, premium & penalty rules"; with rows the button reads "Edit rules" and a row click also opens the grid. Full-page takeover with eyebrow "Deal Memo Setup · Non-union pay rules", intro "Add or edit several pay rules at once — one row per rule…", back arrow titled "Back to pay breakdown".

- **Rows must be complete before Save** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: "Add Rules" → "3 rows". → Fill one row with Rule type, Name and Amount; leave two blank. → Try Save. → "Remove rule" on the blanks.
  - Expected: Counter "1 of 4 ready · 3 incomplete" (seed row included); "Save 4 rules" disabled while any row lacks Rule type, Name or a numeric Amount. After removing blanks: "1 of 1 ready" and "Save 1 rule" enabled. Name stays disabled ("Pick a rule type first…") until a rule type is chosen.

- **Optional columns reach the saved rule** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Row: Rule type an overtime template, Name, Amount 1.5, Day type "CWD", OT Increment 15, tick "Basic + OT on Top", Min/Max Basic Daily Rate, OT Cap, Notes. → Save. → Reopen.
  - Expected: PATCH body row has is_enhancement: true, bare day_type: "CWD", triggers[0].increment: 15, bdr_min/bdr_max on the trigger. An increment of 1 or blank is not written. Hint on the checkbox: "Basic + OT on Top — pays base × (1 + amount) instead of base × amount". Reopening shows the same values; Day type offers Any day, SWD, CWD, SCWD.

- **Editing a seeded row keeps its hidden detail** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Import an agreement with multi-entry triggers (e.g. PACT/BECTU MMP overtime rows) and save. → Reopen, change only the Amount of one row, save.
  - Expected: Only that row's amount changes in the PATCH; its other trigger entries, day_type, less, before, shoot_day are unchanged; rows sharing one id stay separate.

- **Import rules from a union agreement** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: In the grid click "Import union rules". → Agreement picker before a territory. → Pick Territory, then Agreement. → "Import {n} rules". → Close the grid without saving, reopen.
  - Expected: Modal "Import rules from a union agreement"; Agreement is disabled ("Pick a territory first"); the preview groups Overtimes / Premiums / Penalties with "{n} rules ready to import"; an agreement with nothing usable shows "This agreement has no importable rules." and Import is disabled. Import appends all rows to the grid (no dedupe) with note "Added {n}", replacing an untouched seed row. Nothing is saved until "Save {n} rules"; closing discards them.

- **Pickers inside the grid are visible** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub · account: ACC
  - Steps: As ACC open the grid and the Nominal cell's code list. → Open the import modal's Territory dropdown.
  - Expected: Both dropdowns render on top (grid at z-index 1450, above tool windows, below .ah-portal); nothing opens hidden behind the takeover.

- **Default day types** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Scroll below the rules in Non-Union Pay Rules to "Day Types".
  - Expected: Rows SWD (Working min 600, meal break 60), CWD (540, 0), SCWD (570, 30) marked "Default" with no remove control; with no customs: "Only the defaults above apply. Add a custom day type below." A minutes hint like "= 10h" shows beside Working min. No "Save Day Types" button until something changes.

- **Add a custom day type** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: "Add day type": Code LWD, Working min 660, Meal break min 45, Label "10-hour day". → "Save Day Types". → Reload.
  - Expected: PATCH /api/v2/account-hub/project-settings/day-types with the defaults plus { day_type: "LWD", work_min: 660, meal_break_min: 45 }; success toast; the row persists after reload with a "Remove" control. New non-union deals snapshot it.

- **Day type validation** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/setup-hub
  - Steps: Add a row with empty Code, Save. → Code SWD. → Code X, empty Working min. → Working min 1500, then 7.5. → Meal break -5.
  - Expected: Error toasts, in order: "Every day type needs a code (e.g. CWD, SWD)."; "Duplicate day type \"SWD\" — codes must be unique."; "\"X\": working minutes is required."; "\"X\": working minutes must be a whole number between 0 and 1440."; "\"X\": meal break must be a whole number of minutes (or blank)." No PATCH is sent. The 101st row is refused with "A project can have at most 100 day types."

### 07 Deal Memo › Creating & issuing a deal (/film-tools/deal-memo/quick, /edit) › Create a deal (sections)

- **Non-accountant poster sees fewer sections** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: POST
  - Steps: As POST open /quick. → Edit Allowances & Rentals.
  - Expected: No "Nominal Coding" or "Payroll Start Form" sections; the allowance rows have no Nominal column.

- **Territory & Union belongs to the setup** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Expand "Territory & Union". → Switch the setup in the picker.
  - Expected: Shows the setup's Production Entity, Production Type (read-only), territory, agreement and band; no Edit control. Switching setup updates it.

- **Crew type locks after draft** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open an issued deal via its Edit → "Edit Deal Memo". → Look at Crew Details.
  - Expected: Picker and Project/External toggle disabled; tooltip "Locked — the Project/External type can only be changed while the deal is a draft."

- **Employee status and loan-out** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Employee Status → pick a loan-out status. → Enter an invalid company email.
  - Expected: "Loan Out Company" block appears; invalid email shows "Enter a valid email address" and Issue lists "Loan Out Company email — enter a valid email address" (translated). With no statuses: "No employee status present for selected agreement."

- **Half-filled phase blocks Issue** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Turn on "Set Prep / Shoot / Wrap Schedule". → Fill only Prep start. → "Issue Memo". → Clear it so all phases are empty and issue again.
  - Expected: First: Deal Structure — "Prep End Date". Second: "Phase Schedule dates (or turn the schedule off)".

- **Out-of-window phases only warn** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Set Shoot to start before Prep ends, and Wrap to end after the deal end. → Issue.
  - Expected: Row errors "Overlaps with Prep — must start after prep ends" and "Ends after estimated deal end date", banner "Overlapping or out-of-bounds phases will produce incorrect cost estimates." Issue is not blocked by them.

- **Notice period and reminder** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Notice Period → "Custom", value 0, issue. → Set 3 Weeks and reminder 2 Days, save.
  - Expected: Step 1 blocks with "Custom Notice Period". Step 2 saves deal.notice_period = 3_week and notice_reminder = 2_day. Choosing "None" clears the reminder.

- **Date Due drives Chase overdue** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: In Deal Structure set "Date Due" to yesterday. → Issue. → Open All Deals.
  - Expected: "Deal Memo Date" shows today after issue ("Stamped as today's date when the memo is issued."). The row's Chase reads "Chase · Overdue".

- **Mark a field required for the crew** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: crew
  - Steps: In Crew Personal Details mark "Sort Code" required. → Issue. → As the crew, open Send for Approval without a sort code.
  - Expected: The crew's "Not ready yet" list names Sort Code until it is filled.

- **Conditions and work location** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Conditions & Documents → Edit. → "Add Condition", remove a pre-filled one. → Pick Work Location "On Location".
  - Expected: Standard conditions appear pre-filled on a new deal (not on edit). Work Location options "Studio / Local", "On Location", "Mixed". Travel Zone Election shows only for PACT/BECTU TVDA or MMP with distant location applicable; otherwise credit_conditions.travel_zone saves ''.

- **Deleted setup clause comes back on /quick** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: In a setup delete a project standard condition, save. → Create from that setup on /quick.
  - Expected: Code: the builder re-prepends the project condition (no templatePayload guard). Record actual.

- **Custom document upload rules** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: "Upload Custom Document" → drop a PDF. → Drop a .png. → Drop a 21 MB PDF. → Save.
  - Expected: PDF listed under "Attached Custom Documents" with "Uploads on save", then uploaded on Save. PNG: "{name}: unsupported file type. Only PDF files are allowed." 21 MB: "{name}: exceeds the 20MB per-file limit." Each document has a "Crew sign required" toggle.

- **Nominal Coding lines (accountant)** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: As ACC edit "Nominal Coding".
  - Expected: "Labour Nominals" lists Basic Labour plus the deal's OT/premium/turnaround rows and enabled allowances/rentals; no Holiday Pay row. "Tax Credit Tagging" offers the entity's regimes or "There is no tax-credit regimes configured…".

- **Payroll Start Form (accountant)** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Edit "Payroll Start Form". → Pick a bureau, Pay Frequency, First Pay Period Start. → Save.
  - Expected: First bureau pre-selected; with none: "No payroll bureaus configured yet. Add them in Deal Memo → Deal Memo Setup to surface them here." Saved as payroll.bureau, pay_frequency, first_pay_period.

### 07 Deal Memo › Creating & issuing a deal (/film-tools/deal-memo/quick, /edit) › Rates & calculations

- **Manual override and reset** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: After the scale fills, change Day Rate. → Click "Reset to scale".
  - Expected: Note "Rates manually overridden — differs from published scale"; reset restores the scale values.

- **Band required / no rate** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Agreement with bands and no band chosen. → Designation with no published rate.
  - Expected: "Budget Band required." alert pointing to Step 1. With no rate: "No rate available — rate is freely negotiable." and Issue lists Day Rate and Weekly Rate. A typed 0 counts as missing.

- **Phase-specific rates total** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Phases Prep 5 d, Shoot 15 d, Wrap 4 d. → Turn on "Prep / Shoot / Wrap rates": Prep 200, Shoot 300, Wrap 200.
  - Expected: "Total Phase Labour (ex. OT/allow.)" £6,300.00 (200×5 + 300×15 + 200×4).

- **Fringe totals follow HP mode** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Weekly Rate 1500, fringe package with 3% pension and a 12.07% HP-exclusive-only item. → Toggle Exclusive, then Inclusive.
  - Expected: Exclusive: "Estimated total fringe cost (weekly)" £226.05 / wk and "Total weekly cost to production" £1,726.05 / wk. Inclusive: HP item dropped → £45.00 and £1,545.00.

- **Buy-Out and Picture fields** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Deal Type Buy-Out, Rate Basis "Weekly", leave rate empty, issue. → Basis "Both". → Deal Type Picture Deal with fee 0.
  - Expected: Buy-Out weekly: "Buy-Out Rate (weekly)" missing; Both: also "Daily Rate". Picture: "Picture Fee" missing and Payment Currency not required.

- **Contract and payment currency** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Union deal: check Contract Currency. → Non-union deal on a project with no default currency. → Pick a different Payment Currency.
  - Expected: Union: locked to the territory currency. Non-union with no project currency: Issue lists "Contract Currency". Different payment currency shows "Payment currency differs from contract currency. An FX conversion will apply…".

- **Allowance amounts converted to the territory currency** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Project default GBP, territory EUR with exchange rate 1.17 in Project Currencies; project allowance 50.00. → Create a deal. → Remove the EUR rate and repeat.
  - Expected: Allowance shows 58.50 with the alert "Your project default currency (GBP) differs from the territory currency (EUR) — project allowance and rental amounts were automatically converted…". Without a rate: "The territory currency (EUR) has no exchange rate configured…" and 50.00 stays.

- **Per-deal pay rules edit and reset** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: "Pay rules for this deal" → "Edit rules". → Change an OT row's rate, commit, save the deal. → Reload /edit. → Change again, then "Reset" → "Reset rules".
  - Expected: PATCH /deals/:id carries the edited row's source; reload shows the edit. Reset confirm "Discard the rule changes made since this deal was last saved?" restores the saved rules ("Keep changes" cancels).

- **Removed agreement rule after reload** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: In "Edit rules" delete an agreement overtime row, commit, save the deal. → Reload /edit, change any field, let autosave run. → Reload /deals/:id.
  - Expected: Expected: the removed row stays gone. Code hydrates ruleRowRemovals: [], so the row may come back on the next save. Record actual.

- **Non-union deal rules** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Create a Non-Union deal and leave rules untouched. → Save and inspect the PATCH body.
  - Expected: Overtime Structure shows the project Non-Union Pay Breakdown rows. Untouched deal saves empty overtimes/premiums/penalties; day_types include SWD, CWD, SCWD.

### 07 Deal Memo › Creating & issuing a deal (/film-tools/deal-memo/quick, /edit) › Autosave & drafts

- **Later edits patch, bursts coalesce** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Make three edits within 4 s, wait 6 s. → Type continuously for 35 s.
  - Expected: Step 1: one PATCH /deals/:id. Step 2: a PATCH fires by the 30 s ceiling even without a pause.

- **Untouched form writes nothing** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open /quick (with a setup auto-applied). → Leave without editing.
  - Expected: No POST /deals is sent; no new draft appears.

- **Save as Draft asks for a name** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: New external deal with no Crew Name. → "Save as Draft". → Confirm empty, then type a name and "Save Draft".
  - Expected: Modal "Name this Deal Memo" (placeholder "e.g. Sarah Mitchell"); empty shows "Please fill in the name."; then POST/PATCH /deals?notify=true; the button label becomes "Save". A picked project crew member counts as a name.

- **Leaving an existing deal never deletes it** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open an existing draft from All Deals. → Edit, Back, "Leave Without Saving".
  - Expected: No DELETE; the deal stays in All Deals.

### 07 Deal Memo › Creating & issuing a deal (/film-tools/deal-memo/quick, /edit) › Validation & issue

- **/submit refused still lands on the deal** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: page.route POST /deals/*/submit → 422 deal_no_approval_tiers_configured. → Issue.
  - Expected: Error toast; still navigates to /deals/:id; deal saved as "Draft".

- **Re-issuing a rejected deal** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: CREW rejects a deal. → POST opens it ("Edit Deal Memo"), fixes it, "Issue Memo".
  - Expected: /submit decides: per the wrapper docs rejected → awaiting_approval without the crew signing again. Record the resulting status.

### 07 Deal Memo › Creating & issuing a deal (/film-tools/deal-memo/quick, /edit) › Hosted in Settings (Approve New User Request)

- **Send a deal from a joining request** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Admin Settings → "Approve New User Request". → On a requester with no deal open the Deal Memo send menu → "Union".
  - Expected: Embedded /quick: no builder Back/breadcrumb; the crew member is pre-filled and the picker locked; "Save as Draft" and "Issue Memo" remain.

- **Deal then NDA hand-off** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Use the send-both action on a requester. → Issue the deal. → Click "Send NDA".
  - Expected: After /submit succeeds a prompt "Deal memo sent" offers "Send NDA" / "Not Now"; Send NDA opens the embedded Send NDA page for the same person.

### 07 Deal Memo › Deal lifecycle: lists, deal page, signing & approval (/deals, /deals/:id, /my-deal, /approval-queue) › Lists, search, filter, sort

- **All Deals columns** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: POST
  - Steps: As POST open All Deals.
  - Expected: Columns Reference, Crew Member, Department / Designation, Day Rate, Type, Period, Status, actions. Newest first. External deals show an "External" chip. GET /api/v2/deal-memo/deals.

- **Department filter and sort** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: "Dept:" pick a department. → Sort "Date ↑", "Crew A–Z", "Day Rate ↓".
  - Expected: Only that department's rows; sort order matches (created date ascending, crew name A–Z, daily rate high to low).

- **Search** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Type a crew name, then a reference, then a designation. → Click ✕ ("Clear search"). → Type nonsense.
  - Expected: Matching rows only; ✕ clears; nonsense shows "No deal memos match this filter."

- **Delete gating and copy** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Check the delete icon on draft, issued, awaiting, rejected, approved, active rows. → Delete an issued deal.
  - Expected: Icon only on draft, issued, awaiting approval. Issued confirm: "Are you sure you want to delete the ISSUED deal (the crew already have its portal link) \"{name}\"? This action cannot be undone." "Delete" → DELETE /deals/:id; row gone.

- **Chase an issued deal** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: On an issued row (not your own) click "Chase". → Set Date Due in the past on another and look at its button.
  - Expected: POST /deals/:id/chase; success toast; status unchanged. Past Date Due: button "Chase · Overdue", tooltip "Details overdue — chase the crew member to complete them". No Chase on your own deal.

- **Activate from the list** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: On an approved row click "Activate".
  - Expected: "Activating…", POST /deals/:id/activate, success toast, row status "Active".

- **Pending-nominals banner and chip** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: ACC
  - Steps: As ACC with deals issued via "Later". → As POST.
  - Expected: ACC: banner "{n} deal(s) has/have nominal coding pending — … Cost Report's Payroll committed column…" and "Pending Nominals" chips (not on drafts or rejected). POST sees neither.

- **Approvers not configured** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Leave a department with deals without any Deal Memo approver (no All config). → Open All Deals as POST. → Click "Set".
  - Expected: Banner "Approvers not configured" — "No approvers are set for {department}. Deals from these departments cannot be approved until approval levels are configured." "Set" opens /film-tools/deal-memo/approvers (Deal Memo module locked).

- **Configure approvers and return** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: On the Approvers page add a Level 1 approver for that department and save. → Click Back.
  - Expected: Back returns to All Deals; the banner is gone (tiers re-read, GET approval tiers deal_memo).

- **Overview tab** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open "Overview". → Click "View all".
  - Expected: Banner "Deal Memo Overview"; cards Total Deals, Approved, Awaiting Approval, Total Daily Value; "Recent Deal Memos", "By Department", "Status Summary" (Active, Awaiting Approval, Issued, Draft). GET /deals/overview. "View all" opens All Deals.

### 07 Deal Memo › Deal lifecycle: lists, deal page, signing & approval (/deals, /deals/:id, /my-deal, /approval-queue) › Documents & downloads

- **Export the deal register** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: All Deals → "Export" → "Export PDF". → Repeat with "Export Excel".
  - Expected: POST /api/v2/deal-memo/deals/export with the format; a .pdf / .xlsx downloads; drafts are not in it.

- **Export Starter Forms** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: "Export" → "Starter Forms".
  - Expected: Loading toast "Preparing start forms… this can take a minute"; POST /deals/export/start-forms; a ZIP downloads covering all deals regardless of the list filters.

- **View the deal PDF** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: On /deals/:id open the bar's More menu → "View PDF". → After the crew signs, open it again.
  - Expected: PDF opens in the shared viewer (generated via POST /deals/:id/pdf when nothing is stored). After signing the label reads "View Signed PDF" and shows the signed copy.

### 07 Deal Memo › Deal lifecycle: lists, deal page, signing & approval (/deals, /deals/:id, /my-deal, /approval-queue) › Deal page (posting user)

- **Header for a posting user** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open an issued deal from All Deals.
  - Expected: Back chip, "Contracts" eyebrow, reference, status badge "Issued", Edit control. Bottom bar More menu: "Show Checklist" (opens "Checklist for the Crew Member", Item / Status Complete or Pending, sub-line "{outstanding} of {total} outstanding"), "View PDF", "History" ("Deal Memo History", GET /deals/:id/history). No "Send for Approval", "Reject" or "Query" button.

- **Shareable crew link for external deals** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open an issued external deal. → "Copy link". → Open an internal deal and an external draft.
  - Expected: Strip "Shareable crew link" with the URL <origin>/deal-memo/crew-portal/<token> (GET /deals/:id/portal-link); button shows "Copied". No strip for internal deals, drafts, approved or rejected deals.

### 07 Deal Memo › Deal lifecycle: lists, deal page, signing & approval (/deals, /deals/:id, /my-deal, /approval-queue) › My Deal & crew details

- **No deal on file** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: view-only member
  - Steps: As a view-only member with no deal open My Deal.
  - Expected: "No deal memo on file" with "You don't have a deal memo for this production yet…". GET /api/v2/deal-memo/deal.

- **Invalid email/phone blocks save** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Enter "abc" as Email and "12" as Mobile. → Click Save.
  - Expected: Field errors "Enter a valid email address" and "Enter a valid phone number (5–15 digits)"; footer "Fix {fields} before saving or continuing."; no PATCH.

- **No editing on a live deal** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: crew of an active deal
  - Steps: As the crew of an active deal open My Deal. → Open /film-tools/deal-memo/my-deal/complete.
  - Expected: No "Complete your details"; the URL redirects to /my-deal.

### 07 Deal Memo › Deal lifecycle: lists, deal page, signing & approval (/deals, /deals/:id, /my-deal, /approval-queue) › Send / e-sign / countersign

- **Document changed while signing** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open the signer. → page.route the next GET /deals/:id to return a different deal_pdf.attachment.media. → "Sign Document".
  - Expected: Error toast "This document changed while you were signing — it has been reloaded. Please place your signature again."; no sign POST.

- **Start Form optional, required docs mandatory** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: crew sign only the Deal Memo
  - Steps: Attach one additional PDF with "Crew sign required" on and one with it off. → As crew sign only the Deal Memo and press Send. → Sign the required doc.
  - Expected: The Start Form is never required. The required doc is listed under "Documents to sign" until signed (POST /deals/:id/additional-documents/:docId/sign); the informational doc is not.

- **UK PAYE starter answers gate signing** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: UK deal, PAYE employment status, no starter statement or P45. → Click "Deal Memo".
  - Expected: Blocked: "Not ready yet" lists "UK Payroll (HMRC)" — "Starter statement, or P45 details from the previous employer". After answering, signing opens.

- **No approval tiers configured** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Remove the Deal Memo approval tiers (or stub 422 deal_no_approval_tiers_configured). → Send for Approval.
  - Expected: Error toast; status stays "Issued".

- **Crew blocked by uncoded nominals** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Stub send-for-approval → 422 deal_nominals_required_submit. → Send.
  - Expected: Error toast; status stays "Issued"; nothing on the crew screen lets them fix it.

- **Crew edits after signing** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Sign the Deal Memo. → Change a detail and save.
  - Expected: Banner "You have edited your signed deal memo" — "You'll have to sign it again and send it for approval again."; Deal Memo chip unsigned again.

- **Reject offered after sending** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: On an Awaiting Approval deal (crew view) click "Reject" and confirm with a reason.
  - Expected: Code offers Reject on awaiting_approval; the server is documented to answer 422 deal_crew_cannot_reject. Expect an error toast and no status change; record actual.

### 07 Deal Memo › Deal lifecycle: lists, deal page, signing & approval (/deals, /deals/:id, /my-deal, /approval-queue) › Approval

- **Queue Approve & Sign opens the deal** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Click "Approve & Sign" on a row (or the row).
  - Expected: Navigates to /deals/:id; nothing is approved yet (no one-click approve).

- **View-only approver signing** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: AP1 (no posting rights) Approve & Sign.
  - Expected: If the server enforces sign_not_posting_user (403) the sign fails with an error toast and the deal is not approved; record actual.

- **No level 1 for the department** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: POST
  - Steps: Department-scoped config without approvers; deal from that department awaiting approval. → As POST open it. → "Set Approval Level →", add an approver, Back.
  - Expected: Banner "No approver configured for this department" + "Set Approval Level →" → /approvers (state backTo). Back returns to the same deal (history back), and the banner is gone after metadata refetch. Hidden in the Settings embed.

### 07 Deal Memo › Deal lifecycle: lists, deal page, signing & approval (/deals, /deals/:id, /my-deal, /approval-queue) › Edit, revise, amend, activate

- **Draft shows Edit and Issue** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open a draft's /deals/:id (e.g. from a bell link).
  - Expected: Single button "Edit and Issue Deal Memo" → /edit.

- **Add the production's rules to a live deal** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: In the live deal's rules grid look at the toolbar. → Click "Add {n} from Production's Non-Union Pay Rules". → Click it again.
  - Expected: Button lists only rules not already on the deal (tooltip "Adds {n} rules not already here. Rules already on this deal are left exactly as they are."). After adding, note "Added {n} · {m} already here" and the button reads "All of Production's Non-Union Pay Rules added" (disabled). No "Import union rules" button here. Nothing saved until Save.

- **Deal page nominal banner** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: ACC
  - Steps: As ACC open an issued deal with uncoded lines. → Open the same deal on My Deal as CREW.
  - Expected: ACC: "Nominal coding pending" with an "Update Nominals" button. CREW: no banner.

- **Activate from the deal page** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: POST
  - Steps: As POST open an approved deal. → "Activate Deal".
  - Expected: POST /deals/:id/activate; status "Active"; Activate button gone.

- **Creator without posting rights** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Remove POST's posting right after they created a deal. → Open the deal and click Edit.
  - Expected: Code shows Edit to the creator but /edit is posting-only, so they land on My Deal. Record actual.

### 07 Deal Memo › Deal lifecycle: lists, deal page, signing & approval (/deals, /deals/:id, /my-deal, /approval-queue) › Realtime & badges (two users)

- **All Deals refreshes silently** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: POST on All Deals. → In another session ACC issues a deal.
  - Expected: The row appears/updates within a few seconds without reload and without a skeleton flash.

- **My Deal badge and refresh** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: CREW stays on another tab. → POST amends or issues CREW's deal. → CREW opens My Deal.
  - Expected: "My Deal" tab shows a count; My Deal reloads with the change; the count clears on open.

- **Queue badge clears on approve, not on open** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: CREW sends for approval. → AP1 sees the "Approval Queue" count, opens the deal from the queue, goes back. → Approves.
  - Expected: The count is unchanged after just opening; it drops after Approve & Sign.

### 07 Deal Memo › Deal lifecycle: lists, deal page, signing & approval (/deals, /deals/:id, /my-deal, /approval-queue) › Errors, empty & loading states

- **All Deals list failure** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: page.route GET /deals → 500.
  - Expected: Code: rows clear and the table shows "No deal memos match this filter." with no error or retry.

- **My Deal failure** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Stub GET /deal → 500. → "Retry".
  - Expected: "Couldn't load your deal memo." and an error toast; Retry refetches.

### 07 Deal Memo › Notices & deactivation › Notices

- **Send all in a group** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/notices
  - Steps: Click "Send all ({n})". → Confirm.
  - Expected: Confirm "Send all — {group}" ("Each person receives their own notice, using their contract end date as the last pay day…"). One POST per unsent deal; toast; failures "{count} notices failed to send". When all are sent the button is disabled with "All notices already sent".

- **Deactivate offered again after reload** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/notices
  - Steps: After AH-DM-161 reload the page. → Click Deactivate again.
  - Expected: Code: the button returns (session-only hiding); the second call fails with 422 deal_already_deactivated. Record actual.

- **Edit the notice template** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/notices
  - Steps: "Notice Template". → Edit text, insert a placeholder via "Add Placeholders", toggle "Preview". → "Save Template". → "Reset" → confirm.
  - Expected: "Showing example values" in preview. PATCH /api/v2/account-hub/project-settings/notice-template {value}; "All changes saved". Reset asks "Reset to default wording?" then restores the default text.

### 07 Deal Memo › Global Production Rates › Global Production Rates

- **Open the catalogue** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/global-production-rates
  - Steps: As POST click "Global Production Rates".
  - Expected: Full page with "Contracts" eyebrow and title "Global Production Rates", a "Refresh data" button and a sidebar with "Search by country, code, or region…". A skeleton shows until GET /api/v2/deal-memo/branches/covered-territories lands. The top-left back button ("Back to All Deals") returns to /deals; a territory search with no match shows "No territories match."

- **Drill down to an agreement** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/global-production-rates
  - Steps: Pick a covered territory. → Pick a union. → Open an agreement. → Use the in-page Back.
  - Expected: Territory view lists unions/branches; union view lists agreements and a rates table; agreement detail shows Overtimes, Premiums, Turnaround, employment statuses and fringe packages. Back returns to the list it came from.

- **Refresh data re-seeds the catalogue** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: /film-tools/deal-memo/global-production-rates
  - Steps: Hover "Refresh data". → Click it.
  - Expected: Tooltip "Re-seed agreements, unions & designation rates from source data". Label "Refreshing…", POST /api/v2/deal-memo/agreements/import-defaults, success toast; the view resets to the welcome state.

### 07 Deal Memo › NDAs (/film-tools/deal-memo/nda, /my-nda, /nda/:id, /nda/send, /nda-templates) › NDAs — register

- **Row actions by state** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Look at rows with no NDA, sent, declined, signed (your turn and not), completed, legacy.
  - Expected: No NDA → "Send". sent/declined → "Replace" + "View". signed and your turn → "Sign". Otherwise "View". Legacy shows "Sent (legacy)" and a dash.

- **View-only approver register** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: AP1
  - Steps: As AP1 open "NDAs".
  - Expected: Banner "These NDAs are waiting on your countersignature. Open one to sign it."; only NDAs awaiting AP1's level; no filter pills; none → "Nothing is waiting on your signature."

### 07 Deal Memo › NDAs (/film-tools/deal-memo/nda, /my-nda, /nda/:id, /nda/send, /nda-templates) › NDAs — send & templates

- **Unfilled placeholders warning** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Leave a placeholder unfilled. → "Send NDA". → "Go back". → Send again → "Send anyway".
  - Expected: Note "Not filled in yet: {fields}…". Modal "Send with unfilled placeholders?" — Go back cancels; Send anyway sends with the {{TOKEN}} markers intact.

- **Send-to picker** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open /film-tools/deal-memo/nda/send directly. → Search crew.
  - Expected: "Choose who this NDA is for." and the "Send to" picker: only members with Send or Replace actions, never yourself, never signed/completed; "{count} more — type to narrow the list." when long.

- **Send to an external email** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: In "Send to" type bad, then EXT's address. → "Continue", "Send NDA".
  - Expected: "Continue" disabled until the email is valid. Send creates an NDA for EXT; the register shows "External recipient"; EXT receives an email with the NDA Portal link.

- **Replace an unsigned NDA** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: On CREW2's sent NDA click "Replace". → Send.
  - Expected: The same Send NDA page with CREW2 preset; after sending, the register shows the new NDA (latest by sent date).

- **Templates list and delete** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: "NDA Templates". → "Delete" on a template and confirm.
  - Expected: List of templates with "Edit" / "Delete"; empty: "No NDA templates yet. Create one — it starts from the standard NDA wording."; "New template". Delete asks "Delete template?" — "\"{name}\" will no longer be offered when sending. NDAs already sent from it are not affected." then DELETE /nda-templates/:id.

- **Create a template** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: "New template". → Try to save with no name. → Name it, pick a Disclosing party, add a placeholder via "Add Placeholders", save.
  - Expected: Editor starts from the standard wording (GET /nda-templates/default). Save disabled until "Template name" is set. Disclosing party fills {{DISCLOSING_PARTY}}. POST /nda-templates; URL becomes /nda-templates/:id; "Unsaved changes" clears.

### 07 Deal Memo › NDAs (/film-tools/deal-memo/nda, /my-nda, /nda/:id, /nda/send, /nda-templates) › NDAs — sign, my NDA, countersign

- **My NDA with nothing sent** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: member
  - Steps: As a member with no NDA open "My NDA".
  - Expected: "No NDA yet" / "Nothing has been sent to you. If production sends one it will appear here."

- **Recipient fills placeholders** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: CREW
  - Steps: As CREW open My NDA on a letter with fields. → Fill some, "Save details".
  - Expected: "Your details" with "{done} of {total} completed". POST /nda/:id/placeholders; toast "Details saved."; the letter shows the values.

- **Stale countersign attempt** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: AP1 opens /nda/:id. → Another Level-1 approver signs first. → AP1 clicks "Sign NDA".
  - Expected: Info toast "This NDA has changed since you opened it and is no longer waiting on your signature."; no signer.

- **Non-approver opens an NDA** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo · account: CREW
  - Steps: As CREW open another member's /film-tools/deal-memo/nda/:id.
  - Expected: "NDA not available" — "You can open this NDA only if you are one of its approvers."

- **NDA badge goes to one tab** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Send an NDA to CREW and to POST. → Each opens the module.
  - Expected: CREW sees the count on "My NDA"; POST sees it on "NDAs". Opening the tab clears it.

### 07 Deal Memo › Crew portal & NDA portal (public: /deal-memo/crew-portal/:token, /deal-memo/nda-portal/:token) › Crew portal & NDA portal — OTP gate

- **Email must be valid** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Type ext@, then a valid address with spaces around it.
  - Expected: "Send Verification Code" disabled until valid; the request body carries the trimmed email.

- **Wrong email** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Enter a different address.
  - Expected: The server's message is shown on step 1; no code step.

- **Wrong or expired code** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Enter 000000. → "‹ Change email".
  - Expected: Error notice on step 2 (server message); Change email returns to step 1.

- **Session survives reload, not a new tab** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: After verifying, reload. → Open the same link in a new tab.
  - Expected: Reload: no gate (sessionStorage key dm_portal_session:<token>). New tab: gate again.

- **Revoked or invalid link** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open /deal-memo/crew-portal/not-a-token, request a code.
  - Expected: Terminal "Link unavailable." / "This portal link is invalid, expired, or has been revoked." (or "Link Unavailable" after verify).

### 07 Deal Memo › Crew portal & NDA portal (public: /deal-memo/crew-portal/:token, /deal-memo/nda-portal/:token) › Crew portal & NDA portal — crew deal

- **Document title with curly quotes** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Give an additional document the title Crew’s NDA and mark it sign-required. → Sign it on the portal.
  - Expected: Code: X-Filename is not sanitised, so the browser may throw before sending. Record actual.

- **Reject from the portal** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: On an issued deal "Reject", confirm with a reason. → On an awaiting deal try again.
  - Expected: POST …/reject {reason}; status "Rejected". At awaiting approval the 422 shows "This deal can no longer be rejected — it has already moved past your stage."

- **Acknowledge an amendment on the portal** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: POST runs Update Non-Union Pay breakdown on EXT's active deal. → EXT opens the portal, "Acknowledge".
  - Expected: Banner "Your deal memo has been amended"; POST …/acknowledge-amendment; banner gone.

- **Live deal is read-only** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Open the portal for an active external deal.
  - Expected: No sign chips, no "Complete your details"; the memo is read-only.

- **Opening the link next to a signed-in session** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Sign in to Zillit in tab A. → Open the crew portal link in tab B.
  - Expected: Tab A keeps its session (path is in isPublicDeepLink and PUBLIC_ROUTE_PREFIXES).

- **Phone width** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Set viewport 375×812. → Walk the gate and open the portal.
  - Expected: No horizontal scroll; field grids stack in one column.

### 07 Deal Memo › Crew portal & NDA portal (public: /deal-memo/crew-portal/:token, /deal-memo/nda-portal/:token) › Crew portal & NDA portal — NDA portal

- **Fill the letter** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Fill the recipient fields. → "Save details".
  - Expected: Letter from GET …/template; save posts raw HTML to POST …/placeholders/binary; toast "Details saved."; fields keep their values.

- **Download captions** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Before signing click "Download". → After an approver countersigns, download again.
  - Expected: Before: "Unsigned copy" (original /document). After: "Countersigned copy" (/nda-pdf).

- **NDA link next to a signed-in session** — planned (e2e/account-hub/07-deal-memo.md)
  - Where: 07 Deal Memo
  - Steps: Sign in to Zillit in tab A. → Open the NDA portal link in tab B.
  - Expected: Code: /deal-memo/nda-portal/ is missing from isPublicDeepLink and PUBLIC_ROUTE_PREFIXES, so the multi-tab guard may send tab A away and the app may boot authed calls. Record actual.

### 08 Time Card › Time Card › Access & navigation

- **Assigned and Configuration tiles** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open the landing as Completer B (after Crew A assigned a week) and as a user with no assignment. → Open it as Accountant C, then with metadata stubbed to is_accountant:false.
  - Expected: "Assigned Time Card" / "Your crew" only for B. "Time Card Configuration" / "Production setup" shows for C in both cases (auth isAccountant is OR-ed with the server flag).

- **Page description names only reachable surfaces** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: View the landing as Crew A, T1, C, and a user who is both approver and accountant.
  - Expected: Text is respectively "Manage your time cards and daily logins.", "…daily logins and approvals.", "…daily logins and time card configuration.", "…daily logins, approvals and time card configuration."

- **Hidden, unknown and clashing routes** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open /film-tools/timecard/disputes. → Open /film-tools/timecard/foo. → In a browser tab open /film-tools/timecard/history.
  - Expected: Steps 1–2 redirect to /film-tools/timecard (no "Crew Time Card Disputes" tile anywhere). Step 3 renders the generic Tools History page, not the time-card History (which lives at /my-timecards).

- **Assigned / Config / Departments have no route guard** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard · account: Crew A
  - Steps: As Crew A (no assignments, not accountant) open /assigned-timecards, /config, /departments.
  - Expected: Pages render (tile hiding is the only gate); record what the server returns to GET /config, GET /department-completers and GET /assignments/all for this user (don't save).

- **Weekly header buttons and back chips** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open Weekly. → Click "History", then the back chip. → Click "Review Inbox", then the back chip. → Click the back chip ("Back to Time Cards").
  - Expected: History and Review Inbox open as full pages with back chip "Back to Weekly Time Card" returning to Weekly; Weekly's back chip returns to the landing.

- **Metadata failure leaves gates closed** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Stub GET /timecards/metadata → 500. → Open the landing as Approver T1 / Accountant C.
  - Expected: Error toast; Approval Queue and Assigned tiles hidden; Configuration still shows for an auth accountant; Weekly uses a Monday-start week.

### 08 Time Card › Time Card › Daily login

- **Location denied still records** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Deny geolocation in the browser context. → Click "Log In".
  - Expected: Request still sent with lat:null, long:null, location:null, is_gps:false; success modal shows.

- **Address comes from Google reverse geocoding** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Grant geolocation; stub maps.googleapis.com/maps/api/geocode/json → {status:"OK", results:[{formatted_address:"1 Test St"}]}. → Click "Log In".
  - Expected: Login body has location:"1 Test St"; with the stub returning a non-OK status the body has location:null but coordinates kept.

- **Recorded time is browser local HH:MM stamped as UTC wall-clock** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Run with timezoneId:"Asia/Kolkata", clock fixed at 09:05 local. → Click "Log In".
  - Expected: time = today's UTC midnight + 09:05 (reads back "09:05" in every view), not 03:35Z.

- **Today outside the payable window** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard · account: Crew F
  - Steps: Stub GET /deal-memo/deal?status=active,completed,deactivated with start_date tomorrow. → Log in as Crew F (or stub last_pay_date = yesterday).
  - Expected: No buttons. Step 1: "No login to record for today" / "Your deal hasn’t started yet." Step 2: "No login to record for today" / "Your last pay date has passed.", and the landing shows the "No active deal memo" banner.

- **No active deal banner** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard · account: Crew E
  - Steps: Log in as Crew E.
  - Expected: Landing banner "No active deal memo" / "You don’t have an active deal memo. Pay rates, schedule, and entitlements can’t be applied until your deal memo is activated."; buttons still show (missing bounds are permissive).

- **Login request fails** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Stub POST …/login → 422 with a server message. → Click "Log In".
  - Expected: Error toast with the server's translated message; button returns to "Log In"; a second click while in flight sends nothing (spinner, single request).

- **Location switches are UI-only** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Toggle "Turn location on permanently". → Reload.
  - Expected: The two switches behave as a radio pair; after reload "Turn location on when login and turn off when I logout" is on again; no request is sent and login still asks for GPS.

### 08 Time Card › Time Card › Weekly time card

- **Week navigation and its bounds** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Previous week" until it disables. → Click "Go to this week". → Check "Next week" on the current week.
  - Expected: Each move loads GET /weekly?week_starting= (and POST /weekly when none); "Previous week" disables at the week holding deal_start; the "Go to this week" pill shows off the current week and returns to it; "Next week" is disabled on the current week (or on the last_pay_date week if earlier).

- **Unaligned or invalid week_starting** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open …/weekly-timecard?week_starting=<a Wednesday 00:00Z on a Monday-start project>. → Try ?week_starting=abc and a future week.
  - Expected: Expected: rejected or snapped to the period start. Code takes the value raw, so a misaligned (or NaN / future) draft may be created by POST /weekly.

- **Weekly without an active deal** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open Weekly as Crew E.
  - Expected: Week loads and saves; no banner on Weekly; pay panel "No deal memo linked. Pay estimates will appear once your deal memo is set up."; pay figures 0.

- **Non-Monday pay period** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: On a project whose pay period starts Friday, open Weekly.
  - Expected: Week runs Friday → Thursday; week_starting is Friday 00:00 UTC; Daily Login week uses the same 7 days.

- **Add a week note** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Notes". → Type a note, click "Add Note".
  - Expected: Modal "Time Card Notes" ("No notes yet." when empty); POST /weekly/:id/notes {note}; note appears at once with a timestamp; still listed after reload (from tc.notes).

- **Attach, view, replace and remove an invoice** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: As an LTD + UK crew click "Attach Invoice" and pick a PDF. → Click the eye icon. → Click remove.
  - Expected: Upload via the main-app uploadFile, then root-only PATCH /weekly/:id {attachment}; pill shows the file name; eye opens the shared PDF viewer; remove sends {attachment:null}. Hidden for PAYE or non-UK deals.

### 08 Time Card › Time Card › Day types

- **Day-type selector contents** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open a fillable day's type select.
  - Expected: "—" then group "Deal Day Types" (deal codes) and "Other" (built-ins incl. "Half Day", "Idle Day", "Bank Holiday", "Public Holiday", "Non-work Holiday Pay", "Sick (Paid)", "Sick (SSP)", "Sick (Unpaid)"); REST shows as "Day Off"; no bare "Sick".

- **Time-enabled types keep inputs but switching wipes** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: On a filled SWD day pick "Half Day".
  - Expected: Half Day shows time inputs (shoot branch) but the switch cleared the times (Weekly treats only SWD/CWD/SCWD as shoot when switching). Confirm with product whether the wipe is intended.

- **Timecard-only types travel as codes** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Set a day to "Sick (Paid)", Save. → Reload.
  - Expected: PATCH body has day_type:"SICK_PAID" (also HALF_DAY, IDLE_DAY, BANK_HOLIDAY, PUBLIC_HOLIDAY, SICK_UNPAID, SICK_SSP, NON_WORK_HP); after reload the select shows "Sick (Paid)".

- **Paid-not-worked flat days are priced** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Set a fillable day to "Sick (Paid)" (then "Idle Day").
  - Expected: Day Total shows the basic day rate; saved rates_ots has a non-zero "Basic Day Rate" line. Risk: the reference engine's flat set lacks these types; if the deployed bundle matches, the day pays 0.

### 08 Time Card › Time Card › Times, dates, breaks, overnight shifts

- **Unit Call / Unit Wrap have no editor** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Look at the "Unit Call/Wrap Auto filled-in" column.
  - Expected: Values come only from reports ("—" when none); no amend pencil; header hint "Auto fill in your Time Card by clicking on the Production Report".

- **Replace a GPS-logged time** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: On a day with a GPS login, click My Call. → Click "Keep", then again "Replace".
  - Expected: Dialog "Replace login time?" / "This is your login time (HH:MM). Do you want to replace it?"; "Keep" closes; "Replace" opens the amend modal. My Wrap shows "Replace logout time?".

- **Adopt GPS times from "Log in/out"** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: On a day with GPS clock-in and clock-out, click "Log in/out". → Click "Replace".
  - Expected: Modal "Login / logout recorded" with both times and "Do you want to replace your time card entry with this?"; Replace → PATCH /daily-login/<dateMs> with is_manual:false; My Call / My Wrap updated; amendment reason "Adopted logged time".

- **Set a meal break** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click Meal 1 Start on a day, pick 13:00, click "Set Time". → Set Meal 1 End 13:30.
  - Expected: Modal "Meal 1 Start — <day> <date>" with hint "Select the time for your meal break. OT and meal delay penalty will recalculate automatically."; no reason field and no daily-login call; auto-save PATCH carries meals:[{meal_start_time, meal_end_time}].

- **Add and remove meal columns** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "+ Add meal". → Fill Meal 2 on one day. → Click "Remove Meal 1 column".
  - Expected: A Meal 2 column appears on every day; removing Meal 1 shifts Meal 2 into Meal 1; at least one meal column always remains.

- **NDM column** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Add the NDM column" (or "Click here if NDM provided"). → Set "NDM Start" / "NDM End". → Click "Remove NDM column".
  - Expected: NDM column appears with set-time hint "Set the NDM period time for this day."; PATCH has ndm:true, ndm_start_time, ndm_end_time; removing clears NDM on every day.

### 08 Time Card › Time Card › Production-report fill

- **Fallback to the published production report** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Day has no wrap row but a published production report (and another day an unpublished one).
  - Expected: GET {production-report}/v2/production-reports/day-times/range?start&end fills the published day; the unpublished day stays empty.

- **Seed skips the wrong days and never writes My Call / My Wrap** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Have reports for a future day, a Prep day, a Day Off day and a SWD day with empty My Call.
  - Expected: Future, flat-pay and non-paid days untouched; My Call / My Wrap never seeded; untyped past in-range days get the report type (else "SWD").

- **Prod Report panel shows both sources** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Prod Report" on a SWD day. → Open it on a day with no reports (stub).
  - Expected: Side panel with "Wrap Report" and "Production Report" cards, each with "Unit Call / Unit Wrap" and (production only) "My Call / My Wrap"; production times in the report's own timezone, wrap times in UTC. No reports: "No Wrap Report or published Production Report for this day yet. …" and no Fill. The pill is absent on flat-pay, Day Off, disabled and locked days.

- **"Fill from Wrap Report"** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Fill from Wrap Report".
  - Expected: Only Unit Call, Unit Wrap and meals change; My Call / My Wrap untouched.

- **O/C / Per HOD report on an untyped day** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Stub a production report with time_in:"O/C" for an untyped, untimed day. → Open the panel, Fill.
  - Expected: Panel text "The Production Report records O/C for this day (on call / per HOD)." and "Filling sets the day type to Flat — a daily rate with no times."; day becomes Flat with no times.

- **Production fallback read in UTC on a wrap-report day** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: On a BST project, a date has a wrap row without Unit Call and a production report with Crew Call 07:00 BST. → Open the week (seed).
  - Expected: Expected 07:00. Code (mergeDayRow) stamps the merged row "UTC", so the seed may write 06:00.

### 08 Time Card › Time Card › Overtime and pay calculation

- **Camera vs non-camera OT bands** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Camera-department deal with a camera OT row; basic 10 h. → Unit Call 08:00, Unit Wrap 18:30 (meal 1 h → call-to-wrap 9 h 30), My Call 08:00, My Wrap 20:00 (worked 11 h).
  - Expected: No camera OT (call-to-wrap below basic); non-camera OT 60 min (from basic to worked). With Unit Wrap 19:30 (call-to-wrap 10 h 30): camera OT 30 min, non-camera OT 30 min.

- **No times → no basic** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: SWD day with no My Call / My Wrap. → Add a 50.00 daily allowance.
  - Expected: Before: Day Total "—" / 0. After: Day Total 50.00 (allowance only).

- **Meal delay penalty from Unit Call** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Deal meal-penalty row (window 6 h). → Unit Call 07:00, Meal 1 Start 13:30.
  - Expected: Penalty line 30 min "(meal 1) 6h30 after call"; the NDM checkbox appears on the row.

- **NDM suppresses only the first meal penalty** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Same day as above plus Meal 1 End 14:00, Meal 2 Start 20:30. → Tick NDM. → Untick NDM and clear Unit Call.
  - Expected: Step 2: meal 1 penalty disappears; meal 2 penalty (6 h 30 after meal 1 end) stays. Step 3: no meal 1 penalty (meal 1 is measured from Unit Call, now empty).

- **Meal after midnight on an overnight shift** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Unit Call 18:00, My Call 18:00, My Wrap 06:00, Meal 1 Start 01:00.
  - Expected: Expected: a penalty (7 h after call). Code measures meal clock minutes against the same day, so the gap is negative and no penalty fires.

- **Weekly OT threshold** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Deal weekly OT row after 50 h. → Mon–Thu 11 h worked each, Fri 11 h.
  - Expected: Mon–Thu no weekly line; Friday gets a 5 h weekly-OT line ("weekly excess …").

- **Consecutive-day premium across weeks** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Deal 6th-day premium row. → Prior week ended with 3 worked days (prior_week.priorConsecutive 3); work days 1–3 this week. → Set day 2 to Day Off.
  - Expected: Premium fires on day 3 (6th consecutive); with day 2 as Day Off the count resets and it does not fire.

- **Money uses the deal's contract currency** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Use a deal with contract_currency EUR.
  - Expected: Pay figures and OT rate descriptions show €, not £.

- **Accrued holiday pay to date** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open Weekly on a deal with holiday pay. → Save.
  - Expected: GET /weekly/holiday-pay/accrued?through_week=<ms> on open and after Save; row "Accrued Holiday Pay" / "through this week (N days)" with the total; row hidden when the deal has no HP.

- **Engine bundle missing** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Stub the manifest without the agreement and without default.
  - Expected: Weekly still loads; pay reads 0 and the panel says "No deal memo linked"; no crash.

### 08 Time Card › Time Card › Allowances

- **Daily / Weekly view toggle persists** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: In the "Allowances, Rentals, Upgrades & Extras" column switch to "Weekly". → Reload; open Assigned.
  - Expected: Weekly view kept after reload and in Assigned (localStorage['ah:tc:allowanceView']).

- **Day add offers day bases and blocks same-day duplicates** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Expand a day ("View breakdown"), click "+ Add", pick "Allowance", add a deal per-diem. → Open "+ Add" on that day again, then on another day.
  - Expected: Modal "Add to <day>" listing deal items with a daily cadence; custom basis offers "Daily" and "Per Mile" only; item saved to that day's days[].allowances[]. Reopened on the same day the item shows "Added" (disabled); another day can add it.

- **Week add offers week bases** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: In Weekly view click "+ Add for week", pick "Rental" then "Allowance".
  - Expected: Rental basis "5 Days Week" / "3 in 5"; Allowance also "Per Mile"; saved to root weekly_allowances[] with qty and currency.

- **Per-mile allowance** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Day add, Allowance, custom "Mileage" rate 0.45, basis "Per Mile", 30 miles.
  - Expected: Row "Mileage (30mi)" 13.50; PATCH day allowance rate_amount:13.5, qty:1, basis:"mile".

- **3 in 5 rental and paid-not-worked days** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Rental 300.00 basis "3 in 5". → Have 4 worked days, then 2. 3. With a 5 Days Week rental, set one of 4 worked days to "Idle Day".
  - Expected: 300.00 with 4 days (capped at 3); 200.00 with 2. The Idle Day does not count toward the cap ("3 of 5 days") but can still hold a daily allowance.

- **Which days take line items** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Add an allowance to a Prep day; save; reload. → Try to add to a Day Off row.
  - Expected: Prep allowance persists after reload; Day Off rows offer no add (non-paid days refuse items).

- **Deal rentals auto-seed on worked days only** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Deal daily rental 18.00; week with 4 worked past days, future days untyped.
  - Expected: Seeded on the 4 days only (72.00), never on future/untyped days; capped by GET /weekly/rentals/cumulative-claimed?user_id=.

- **Removed seeded rental stays removed in this browser** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Remove a seeded rental (× "Remove … (all days)"). → Reload. → Open the week in a fresh browser context.
  - Expected: Stays removed after reload (localStorage['ah:tc:<id>:suppressedRentals']); re-seeded in the fresh context.

- **Upgrades & Extras counted once** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Add a daily "Upgrade" 25.00 on Monday and a weekly extra 40.00.
  - Expected: Monday Day Total includes 25.00; Weekly Pay Estimate includes 25.00 + 40.00 once; PATCH has days[].additional_fees[{kind:"upgrade"}] and weekly_additional_fees.

- **Capped week row with nothing worked** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Capped weekly rental on a week with 0 worked days. → Save and reload.
  - Expected: Expected 0.00 (qty 0). Weekly coerces qty 0 to 1 on hydrate and save, so it may show and save the full week.

### 08 Time Card › Time Card › Autosave & drafts

- **Network failure retries then stops** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Abort PATCH /weekly/* (route.abort). → Edit.
  - Expected: "Couldn't save — retrying" with retries at about 5 s, 10 s, 20 s; then "Couldn't save" with a "Retry" link that resends.

- **4xx is not retried** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Stub PATCH /weekly/* → 422. → Edit.
  - Expected: Straight to "Couldn't save" + "Retry"; only one request until a new edit or Retry.

- **Leaving during a slow save** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Delay PATCH /weekly/* by 5 s. → Edit, wait for "Saving…", click "Previous week".
  - Expected: After about 1.2 s dialog "Still saving" / "Your last change is still being saved…" with "Leave anyway" and "Stay"; Leave aborts and navigates, Stay keeps the save running.

- **Auto-save off once a tier has approved** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard · account: Crew A edit a day
  - Steps: Submit; T1 approves. → As Crew A edit a day, then click the back chip.
  - Expected: No auto-save pill or PATCH; leaving shows "Unsaved changes" / "You have unsaved changes on this page. Leaving will discard them. Are you sure?" with "Leave" / "Cancel".

### 08 Time Card › Time Card › Validation

- **Custom allowance row validation** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: In an add modal click "+ Add Custom". → Leave Name empty, Rate 0, click Save.
  - Expected: Field errors "Name is required" and "Rate must be greater than 0"; footer "Complete every field on each row to save"; nothing added.

- **Per-mile needs miles** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Custom row basis "Per Mile", miles 0.
  - Expected: "Miles must be greater than 0"; Save blocked.

- **Invoice must be a PDF** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Attach Invoice", pick a .png.
  - Expected: Toast "Please choose a PDF file."; no upload or PATCH.

- **Empty note** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open "Notes", click "Add Note" with only spaces.
  - Expected: Warning "Note can't be empty."; no request.

- **Submit has no completeness check** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: On a draft with empty days click "Submit for approval".
  - Expected: Submit succeeds (empty days and a 0-pay week go through). Spec asks for a completed week; see Open questions.

### 08 Time Card › Time Card › Submit & approval

- **Batch approve** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard · account: T1 tick two rows
  - Steps: As T1 tick two rows ("Select"). → Click "Approve 2".
  - Expected: "2 selected" bar; POST /weekly/batch/approve with both ids; selection clears; list reloads.

- **Saving an approved week** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: On an approved week edit and click "Save".
  - Expected: No confirm; PATCH sent. Record the resulting status and approvals (server behaviour unclear).

- **Queue search, sort and selection** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Type part of a name, role, department in "Name, role, dept…", then "zzz". → Pick "Week starting (oldest)" and "Name (A → Z)". → Click "Select all", then "Deselect all" / "Clear selection".
  - Expected: Filters on name / role / dept; "zzz" → "No timecards match" / 'Nothing matches "zzz".' / "Try a different search term.", and "Clear search" restores. Order changes (week-less cards sink in both week orders). Select-all ticks visible rows only.

- **Detail panel extras are read-only** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Select a card with an invoice and report data. → Click the invoice button; hover a SWD day and click "Production Report" / "Log in/out".
  - Expected: Invoice opens in the shared PDF viewer; the report / clock panel opens with no Fill or Replace buttons.

### 08 Time Card › Time Card › Query / reject / resubmit

- **Batch reject with one reason** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard · account: T1 tick 2 rows
  - Steps: As T1 tick 2 rows, click "Reject 2".
  - Expected: Modal "Reject 2 time cards" / "The same reason will be applied to all 2 timecards…"; POST /weekly/batch/reject {ids, reason}.

### 08 Time Card › Time Card › Payroll lock (owner view)

- **Notes readable but closed on a locked week** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: On a locked week click "Notes". → On an approved week add a note.
  - Expected: Locked: existing notes listed, "This timecard is locked — new notes can no longer be added."; approved: note can be added.

- **Unknown status label** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Stub GET /weekly with status:"processing".
  - Expected: Expected: the raw status. Weekly's header pill falls back to "DRAFT" (History shows the raw word).

### 08 Time Card › Time Card › Assigned time cards & Review Inbox

- **Assign only on draft / rejected and until covered** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Check "Assign" on draft, rejected, awaiting_approval, approved. → Assign "All weeks". → Set one day to "Sick (Unpaid)".
  - Expected: Shown only for draft and rejected; hides once every assignable day is covered. Days set to Sick still count as needing a completer (only REST/Holiday excluded).

- **Out-of-range and future days** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open a week where the assignment covers only part of the week and includes today.
  - Expected: Days outside the assignment show "Outside assignment range", future days "Not yet available"; both read-only; "Pending Only" hides them.

- **Owner never opened the week** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Assign "All weeks"; as B open a week Crew A never opened; click submit.
  - Expected: Toast "No timecard found for this crew member — save first."; no request.

- **Per-day Submit after a partial review** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: With some days already under review and one returned, click that day's "Submit".
  - Expected: Footer submit hidden; per-day "Submit" sends save-and-submit with one date in submit_dates.

- **Completer note reaches the owner** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard · account: Crew A
  - Steps: Type in the day's Note ("<name> sees this on review…"), submit. → As Crew A open Review Inbox.
  - Expected: Note shown under "Note by Completer" for that day.

- **Frozen days and locked card** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Try to edit an under-review day (incl. Prod Report Fill and Log in/out Replace). → Open a card whose week is approved.
  - Expected: Under-review / approved days have no inputs and programmatic fills are refused; on an approved+ card every day is read-only and the bar shows a locked label; "Save Draft" toasts "This timecard is locked and can no longer be edited."

- **GPS adopt writes to the crew member, not the completer** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: On a day with Crew A's GPS times click "Log in/out", "Replace". → Save Draft.
  - Expected: Times land in the draft and go out in PATCH /daily-login/batch with Crew A's user_id; no PATCH /daily-login/<date> (caller-scoped) is sent.

- **Assigned seed for a UTC-negative owner** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard · account: B open it
  - Steps: Crew A creates the card in a America/New_York browser. → As B open it with a wrap report on Tuesday.
  - Expected: Expected: Tuesday's report on Tuesday. Code uses the card's root timezone for the seed's day keys, so values may land on Wednesday.

- **Approve or return the whole week** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Approve All Days", then "Confirm & Approve". → On another week click "Return All to Completer", try an empty note, then enter "Return notes (required)" and click "Return to Completer".
  - Expected: Confirm text "You are approving N days completed by <names>…"; POST /review/<id>/days/bulk-approve {dates} with every under-review day. Return: empty → "Please add a return note first."; then POST …/bulk-return {dates, return_note}.

### 08 Time Card › Time Card › Lists, search, filter (History)

- **History page** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: From Weekly click "History".
  - Expected: GET /weekly/my-summary; tiles "Weeks worked", "Days worked", "Total paid", "Outstanding"; sections Prep / Shoot / Wrap with "Completed" / "Active" / "Upcoming"; current week expanded and scrolled to; notification:read for my_timecards.

- **Filter chips and search** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Draft", "Pending", "Approved", "Rejected", "Paid", "All". → Type a week number, a date fragment, a status word, a day type in "Search weeks…".
  - Expected: Counts are from the full list; each chip filters by lifecycle phase; no "Queried" chip. Search shows matching weeks only; clearing restores.

- **Expand a week and open it** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Expand a past week. → Click "View Full Timecard".
  - Expected: GET /weekly/:id; breakdown "Core pay", "Overtime & turnarounds", "Premiums & penalties", "Allowances, rentals, upgrades & extras", "Est. Gross" (holiday pay noted "accrual · not in Est. Gross"); the button opens /film-tools/timecard/weekly-timecard?week_starting=<ms> on that week.

- **Posted week is not a draft** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Have (or stub) a posted week and a stray empty draft for the same week.
  - Expected: Shows "Posted", counts in "Total paid" and under "Paid"; the draft does not replace it.

- **Payslip download** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Expand a paid or posted week, click "Payslip".
  - Expected: "Generating…"; POST /api/v2/payroll/runs/payslip returns a PDF blob downloaded by the browser; on failure nothing visible (only logged).

- **Buttons with no action** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Expand the current week, click "Submit for Approval". → Stub a week with a query note; click "View thread" / "Mark resolved".
  - Expected: Expected: an action. Code has no handler on these buttons, so nothing happens.

### 08 Time Card › Time Card › Configuration

- **Configuration page** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard · account: Accountant C
  - Steps: As Accountant C open "Time Card Configuration".
  - Expected: GET /config; heading "Time Card Configuration"; side nav "Control Model", "Department Setup", "Approval Chain" with status chips; stored model card selected.

- **Hybrid default** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Hybrid". → In "Hybrid — Default Model" pick "Option A — Production-Controlled", pick a controller, save. → Switch to "Option C — Crew-Controlled", save.
  - Expected: First PUT hybrid_default:"production" with the controller; second hybrid_default:"crew", production_controller:null.

- **Reset to defaults** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Reset to defaults", accept the browser confirm. → Reload.
  - Expected: Browser confirm "Reset all configuration to defaults? This cannot be undone."; local state Hybrid / crew; toast "Configuration reset to defaults."; reload shows the old saved config (nothing sent). Uses a banned browser dialog.

- **Leaving with unsaved changes** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Change the model, click the back chip (Config) or "Back" (Department Setup).
  - Expected: Expected: an unsaved-changes prompt. Code navigates away silently and the change is lost.

- **Department search and clear** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Type in "Search departments…"; type "zzz". → Open a completer picker, click "Clear assignment".
  - Expected: List filters; "No departments match your search."; clearing removes the completer (row back to "Assign").

- **Approval Chain opens the timecard approvers** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Click "Open Time Card Approvers". → Use the back control.
  - Expected: /film-tools/timecard/approvers (Approvers module fixed to timecard); back returns to /film-tools/timecard/config.

- **Config refreshes on another user's save** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open Config as C in two sessions. → Save a change in one.
  - Expected: Other session refetches GET /config (timecard:settings:updated) and shows the new model.

### 08 Time Card › Time Card › Rights

- **Revoking view access while inside the tool** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open Weekly as Crew A. → Admin removes timecard_tool view access (or Tool Customization removes the tool). → Trigger a rights refresh.
  - Expected: Crew A is redirected to /film-tools.

- **Posting access has no visible effect** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Give a non-accountant crew posting_access on timecard_tool. → Open the landing.
  - Expected: No extra tile (Disputes is hidden); same tiles as view-only crew.

### 08 Time Card › Time Card › Realtime & badges

- **Submit reaches the open queue** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: T1 has Approval Queue open. → Crew A submits.
  - Expected: T1's list refetches without reload (timecard:received_for_approval → GET /weekly/approval); landing tile badge counts it.

- **Approve / reject reaches the open Weekly** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Crew A has the week open. → T1 rejects.
  - Expected: Weekly refetches silently (ah:timecard:detail:<id>): pill "REJECTED" and rejection card, no loader flash.

- **Assignment reaches the completer** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: B has Assigned open. → Crew A assigns / unassigns a week.
  - Expected: B's list refetches (timecard:assigned_to_completer / unassigned_from_completer); a dirty open row is not clobbered.

- **Weekly tile and header badges** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Produce unread my_timecards and received_inbox notifications for Crew A.
  - Expected: Weekly tile badge = sum of both; Weekly's "History" badge (tooltip "N unread update(s) on your time cards") and "Review Inbox" badge ("N time card(s) awaiting your review") split them.

- **Approval-tier change updates gates** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Add Crew X as a timecard approver tier while X has the landing open.
  - Expected: Metadata refetches (approval_tier:* with module:"timecard"); "Approval Queue" tile appears without reload.

### 08 Time Card › Time Card › Errors, empty & loading states

- **Week fetch fails** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Stub GET /weekly → 500.
  - Expected: Error toast; empty 7-day grid; no invoice carried over from the previous week.

- **Submit or approve fails** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Stub POST /weekly/*/submit → 422 with a message; click "Submit for approval". → As T1 stub POST /weekly/*/approve → 403 and click Approve.
  - Expected: Server message toast; status stays "DRAFT" and Submit stays visible. Approve: error toast; card stays selected with Approve enabled.

- **Loading states** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Delay deal, week, queue and inbox requests.
  - Expected: Weekly: "Loading deal memo…" overlay then a grid skeleton; landing: Daily Login skeleton (no flash of buttons); History: week-group skeleton; Router fallback "Loading…".

- **Empty approval queue** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Stub GET /weekly/approval → [].
  - Expected: "No time cards waiting on you" / "When a crew member submits a timecard for your level it will land here."; detail "No timecard selected".

### 08 Time Card › Time Card › Theme & windowed view

- **Dark theme** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Toggle the theme on the landing. → Visit Weekly, an amend modal, the Prod Report panel, Approval Queue, Assigned, Config.
  - Expected: All surfaces readable in dark (text, pills, skeletons, modals).

- **Tool window** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open Time Card from the Tools grid as a window. → Open an amend modal and the time picker.
  - Expected: Window opens maximized; sub-routes work inside it; the time picker popup sits above the modal.

- **Narrow viewport** — planned (e2e/account-hub/08-timecard.md)
  - Where: /film-tools/timecard
  - Steps: Open the landing at 375 px.
  - Expected: Daily Login and tiles stack; Weekly grid scrolls horizontally rather than squashing.

### 09 Payroll › Payroll › Access & navigation

- **Tile opens full-bleed; unknown tile falls back** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: A-PLAIN
  - Steps: As A-PLAIN, click "Payroll Processing". → Click "Back to Payroll". → Open /film-tools/account-hub/payroll/foo.
  - Expected: Step 1: no Account Hub sidebar, breadcrumb "Payroll" / "Payroll Processing" (first part rendered uppercase). Step 2: landing. Step 3: redirected to /film-tools/account-hub/payroll.

- **Run header links** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open /payroll/run. → Click "Go to Payroll". → Go back, click "Back to Payroll".
  - Expected: Step 2 lands on /payroll/processing; step 3 on the landing.

### 09 Payroll › Payroll › Rights

- **Hub entry needs the hub licence, tool entry needs the payroll licence** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: As P-VIEW without account_hub_tool, open /film-tools/account-hub/payroll (no marker). → Open /film-tools/account-hub/payroll?entry=tool.
  - Expected: Step 1 bounces to /film-tools (view gate). Step 2 renders the producer landing.

- **Revoking access mid-session ejects the user** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: P-VIEW opens Producer Board from the tool tile. → An admin removes payroll_tool view access.
  - Expected: The page navigates to /film-tools (in Windowed View the window closes) without a reload.

- **Producer can open accountant tiles by URL** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: P-VIEW
  - Steps: As P-VIEW, open /payroll/run?entry=tool, /payroll/processing, /payroll/accountant-payroll. → Open a locked row in Run.
  - Expected: Screens render (no tile guard). Record which actions show: Run row "Mark Paid" on locked, drawer "Add Claims"/"Add Deduction", "Export Summary"/"Export CSV", History "Download PDF". Confirm the server refuses the writes.

- **Posting access is not used** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Give P-VIEW posting_access on payroll_tool. → Repeat AH-PAY-003 and AH-PAY-010.
  - Expected: Behaviour identical to view-only: usePayrollRights().canPost gates nothing.

### 09 Payroll › Payroll › Entry setup / configuration

- **Dirty state and Save & Apply** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open the page. → Toggle "Group by pay category". → Toggle it back.
  - Expected: "Save & Apply" is disabled until a change; pill reads "Unsaved changes" after step 2 and "All changes saved" after step 3 (draft equals saved).

- **Approver picker lists the accounts team only** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open "Approvers".
  - Expected: Only department_accounts users are offered; note "Picker shows accounts-department users only. Other roles aren't eligible to approve payroll on this project."

- **Description Format** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Pick "Week Dd-Dd Mon Yyyy Crew Name Pay". → Save. → Open the Run Journal Ledger.
  - Expected: Saved journal_description_format: "title"; ledger descriptions are title case (see AH-PAY-095).

- **Journal Grouping** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Turn on "Group by pay category". → Save.
  - Expected: Saved journal_group_by_category: true; ledger merges lines per category (AH-PAY-096).

- **Defaults with no saved settings** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub GET payroll-settings → { data: {} }. → Open the page.
  - Expected: "Pay Window" "Mon → Sun"; uppercase format selected; grouping off; Approvers status "Not set".

- **Load and save failures** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub the GET → 500. → Reload; stub the PATCH → 422 and save.
  - Expected: Step 1: red card with the server message or "Failed to load payroll settings"; no sections. Step 2: server error toast; "Unsaved changes" stays.

- **Payroll Group create, edit, delete** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: "Add group" → pick an "Accountant" and a department. → "Create group". → "Edit" → add a designation → "Save group". → Delete it with the trash button.
  - Expected: POST, then PATCH /:id, then DELETE /api/v2/payroll/payroll-groups/:id; list refreshes each time ("Payroll Groups · N"). Empty state "No payroll groups yet — assign crew to an accountant." returns.

- **Payroll Group delete has no confirmation** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Create a group. → Click the trash button ("Delete group").
  - Expected: Deleted immediately; no confirm dialog. Confirm with product whether that is intended.

- **Add a Payroll Account** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: In "Payroll Accounts", click "Add / Edit". → "Add row"; fill Code, "Cost type", "Display name". → Click "Save 1". → Remove it afterwards (AH-PAY-025).
  - Expected: Overlay "Payroll · Settings" / "Payroll accounts"; "Cost type" offers no Expense; PATCH /api/v2/account-hub/payroll-settings/custom-accounts with a create row (no id, code upper-cased); overlay closes; code and name listed. "Add / Edit" is disabled while loading.

- **Remove a Payroll Account** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Click the trash button on a code. → Confirm "Remove". → Repeat on a code that is in use.
  - Expected: Confirm "Remove payroll account" with "Remove {code} from payroll accounts? This deactivates the code in the Chart of Accounts…"; PATCH …/custom-accounts [{ id, status: "delete" }]; row gone. In use: server 422 toast, row stays.

- **Removing inside the overlay asks first** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: "Add / Edit". → Untick "Active" on one row.
  - Expected: Submit reads "Save · 1 removed"; clicking it opens "Remove payroll account" ("1 account will be deactivated in the Chart of Accounts…") before the PATCH.

### 09 Payroll › Payroll › Processing (pull time cards, edit lines)

- **Current-week views** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Click "Day View", "Week to Date", "Weekly Total".
  - Expected: GET /api/v2/payroll/weekly/processing; no week navigator; Day View day pills after today are disabled ("Day hasn't happened yet") and rows for crew not working that day are hidden.

- **Sidebar filters, search, department tabs** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Click "Pending", then "Approved". → Type a crew name in "Search crew, role…". → Click a department tab.
  - Expected: Counts beside "All Time Cards" / "Pending" / "Approved"; "Approved" includes approved, paid, unpaid; search matches name, role, department; tab narrows rows.

- **Send eligibility and select all** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open a crew member with mixed statuses.
  - Expected: Checkboxes only on weeks before approved; footer "Select all N" toggles to "Clear selection"; send button disabled with title "Tick the weeks you want to send" until one is ticked.

- **Send partial failure** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Tick two weeks. → Stub the second POST → 422 with a message. → Confirm.
  - Expected: Error toast "1 sent, 1 failed: W/E {date} — {message}"; confirm stays open; list refetched.

- **Send hidden for non-accountants** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: P-VIEW
  - Steps: As P-VIEW open /payroll/processing → Outstanding → a row.
  - Expected: No week checkboxes, no "Send to Payroll Run".

- **Override visibility** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open the same week as A-APPR, as P-VIEW, and on a draft / approved week as A-PLAIN. → Stub GET /api/v2/payroll/timecards/metadata → 500 as A-PLAIN.
  - Expected: No Override in any case: approvers use the Approval Queue, non-accountants never get it, drafts need Send to Payroll Run, and a failed metadata read hides it.

- **Processing row vs drawer role gate** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: A-PLAIN
  - Steps: As A-PLAIN (not a final approver), open "Weekly Total". → Open the drawer of a locked row.
  - Expected: Rows show only "View"; the drawer footer still offers "Mark Paid" (no role check). Confirm the intended rule.

### 09 Payroll › Payroll › Claims & deductions

- **Add Claims modal content** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Drawer of an approved week → "Add Claims".
  - Expected: Title "Claims · {name}", subtitle "Pending reimbursements fold into the weekly payroll run on approval."; sections "On this payroll" and "Available to add" with item counts and totals; GET /api/v2/cash-expenses/claims/pending-for-payroll?user_id=.

- **Deduction reduces net, not gross** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: On a week with £1,000.00 gross add a £50 deduction. → Read the Processing "Week Summary" and the Run row.
  - Expected: "Total payroll" (net) drops by £50; Run "Gross" stays £1,000.00; "Holiday Pay" and "Total Cost" unchanged.

- **ACCT Approved and Unpaid weeks still take writes** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Add a manual claim on a final_approved week. → Repeat on an unpaid week.
  - Expected: Both succeed; no read-only banner.

- **Read-only claims modal still offers "+ Add"** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Run drawer of a locked week with pending payroll claims → "Show Claims". → Click "+ Add" under "Available to add".
  - Expected: Expected: no add on a read-only week. Code renders the button; the server answers 422.

- **Outstanding modal on a locked week** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Outstanding → crew with a locked week → "+ Add Claims" on that card. → Add a manual line.
  - Expected: No lock banner, "Add line" enabled; save fails with the server's 422 inline; no "Unlock this week" button.

- **"Attach receipt" does nothing** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: "Add a manual line" → click "Attach receipt".
  - Expected: No file picker, no request.

### 09 Payroll › Payroll › Validation

- **Manual claim line rules** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Leave Description empty, Amount 10. 2. Description "   ", Amount 10. 3. Description "X", Amount 0, then -5, then blank.
  - Expected: "Add line" stays disabled in every case; enabled only with a non-blank description and amount > 0.

- **Deduction rules** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Repeat AH-PAY-051 inputs in "Add Deduction".
  - Expected: "Add deduction" disabled unless description is non-blank and amount > 0. Nominal code is optional.

- **Payroll Group save rules** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: "Add group" with only an Accountant. → Only a department, no Accountant. → Accountant + one crew member.
  - Expected: "Create group" disabled in 1 and 2, enabled in 3. Accountant picker lists accounts-team users only.

- **Pay period not seven days** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub settings with pay_period { start_day_of_week: 1, end_day_of_week: 5 }. → Open "Pay Period".
  - Expected: Red warning "This isn't a 7-day window…".

- **Journal effective date floor** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: With a cost-report lock date of 31 Oct 2026, open the Journal Ledger. → Type 15 Oct 2026 into the header "Effective date".
  - Expected: Value clamps to 01 Nov 2026; title "Must be on or after 2026-11-01 (after the last closed period)"; every editable row gets that date.

- **Post modal date minimum** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open "Post to Ledger". → Inspect "Default effective date".
  - Expected: Defaults to today; min = day after the cost-report lock. An earlier typed date is only refused by the server on post.

### 09 Payroll › Payroll › Calculations, totals & currency

- **Weekly quantity rules** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub weekly rows: rate 100 with qty missing; qty 0; qty 0.8.
  - Expected: Adds £100.00, £0.00 and £80.00 respectively (blank qty = 1, real 0 stays 0, capped-week fractions kept).

- **Upgrades & Extras counted once** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub a card with weekly_additional_fees: {} and legacy root additional_fees [{ rate_amount 40, qty 1 }]. → Open Run row and drawer.
  - Expected: Gross includes £40 once; drawer shows an "Upgrades & Extras" line. With both fields populated, still counted once (weekly wins).

- **Holiday pay, NIC and total cost estimates** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub a card: gross £1,000.00, total_days 5, no fringes_total. → Stub a second with fringes_total 200.
  - Expected: Card 1: Holiday Pay £120.70 (12.07%), NIC £123.75 (15% of 1,000 − 175), Total Cost £1,244.45. Card 2: HP £200.00, NIC 0, Total Cost £1,200.00. Read them from the "Holiday Pay" and "Total Cost" summary tiles (compact value; full figure in the tile tooltip).

- **Net uses the deductions scalar first** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub deductions_total 75 and deductions rows summing 50.
  - Expected: Net = gross − 75; gross unchanged.

- **Stat-card currency by scope** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Processing "Weekly Total" with one USD card that has no allowances. → Add a GBP card (project default GBP, USD rate 0.8). → Remove the USD rate.
  - Expected: Step 1: tiles show "$0". Step 2: totals converted to GBP and shown with "£". Step 3: tooltip ends "· some amounts had no exchange rate (added at face value)". Zero-amount lines never flip a row label to another currency.

- **Est. Gross prefers the backend figure** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open "View Time Card" on a card with total_gross 900 and frontend lines summing 950, plus holiday pay.
  - Expected: "Est. Gross" £900.00; "Holiday Pay" shown separately, not added. Without total_gross/gross, the frontend sum (including extras) is used.

- **Weeks are UTC** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Set the browser zone to Asia/Kolkata and clock to Monday 02:00 local. → Open Run and Producer Board.
  - Expected: "Current week" is still last week (rolls over at 00:00Z); week_starting in requests is a 00:00Z epoch.

- **Wednesday pay period** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: On the Wednesday-start project open Run, History, Producer Board, Production Report Payroll.
  - Expected: Run and History open on the last completed Wed–Tue week; the boards on the current Wed–Tue week; Processing pinned to the current period.

- **History gross leaves out Upgrades & Extras** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Use a paid week that has extras. → Compare Run drawer gross with History "Gross Total" / "Week Gross Total".
  - Expected: History is lower by the extras amount. Confirm intended.

### 09 Payroll › Payroll › Run & approval

- **Week change reloads queue and ledger** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Select two rows. → Click "Previous week".
  - Expected: GET /api/v2/payroll/weekly/:ws/processing, POST /api/v2/payroll/runs/journal-ledger/fetch, GET /api/v2/payroll/runs/journal-ledger?week_starting=; selection cleared.

- **No approval role** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: A-PLAIN
  - Steps: As A-PLAIN open Run.
  - Expected: approved and final_approved rows show "View"; toolbar shows no approval button even with rows selected.

- **PA/FC selection with only ACCT Approved rows** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: As A-PAFC select only final_approved rows.
  - Expected: Toolbar button flips to "Lock · N".

- **Toolbar and select-all basics** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open Run with nothing selected. → Tick some rows, then the header checkbox twice.
  - Expected: Only "Export Summary" and "Export CSV" show with no selection. Header box is indeterminate with some visible rows ticked; it selects all visible, then clears them.

- **Drawer footer by status and role** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open drawers of final_approved (as A-FA), locked (as A-PA and A-FA), paid rows.
  - Expected: final_approved for a non-payroll-accountant: no action. locked: "Unlock" (payroll accountant only) beside "Mark Paid". paid: no action button.

- **Override from a Run row** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: As A-PLAIN click "Override" on an awaiting_approval row. → Confirm.
  - Expected: Override modal (as AH-PAY-036); row shows "Overriding…" then "Approved".

- **Server skips or refuses** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub batch/final-approve → { marked: [], skipped: [id] }. → Stub mark-paid → 422.
  - Expected: Step 1: row stays "Approved" after refetch. Step 2: server error toast; row unchanged.

- **Resolve Query changes nothing on the server** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: A-FA
  - Steps: As A-FA open a queried drawer → "Resolve Query". → Reload.
  - Expected: Drawer closes, row shows Approved locally; no request; after reload "Queried" again.

- **Status filter and summary with ACCT Approved / Locked** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Click sidebar "Pending", then "Approved". → Read the "Approved" summary tile.
  - Expected: Code buckets final_approved and locked as "pending": they appear under "Pending", not "Approved", and the tile ("… of N") excludes them and unpaid. Confirm intended.

- **Toolbar Mark Paid ignores the final-approver rule** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: A-PLAIN select an unpaid row
  - Steps: As A-PLAIN select an unpaid row.
  - Expected: Row offers only "View", but the toolbar offers "Mark Paid · 1".

### 09 Payroll › Payroll › Export / print / download

- **Export Summary** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Run → "Export Summary" → PDF. → Repeat with Excel.
  - Expected: Trigger shows "Exporting…"; POST /api/v2/payroll/runs/export-summary { week_starting, format }; file downloads.

- **Export CSV with an approve/lock nudge** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: A-PAFC
  - Steps: As A-PAFC on a week with approved rows click "Export CSV". → Click "Final Approve & Lock N & Export".
  - Expected: Modal "Accountant-approve & lock N timecards?" with postscript "The payroll CSV downloads as soon as this completes…"; runs final-approve + lock, refetches, then export-summary with format: "csv".

- **Export Only, Cancel, no nudge** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open the nudge → "Export Only". → Open it → close. → As A-PLAIN click "Export CSV".
  - Expected: Step 1: CSV downloads, statuses unchanged. Step 2: nothing downloads. Step 3: no nudge, direct download.

- **Run export failure** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub export-summary → 500.
  - Expected: Error toast with the server message (fallback key export_failed).

- **Processing Export CSV** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Processing sidebar "Export CSV". → Pick "All Outstanding till last Pay week". → Repeat with "Current Week".
  - Expected: Modal "Export Payroll CSV" with those two options only (no "Selected Week"); toast "Generating CSV" → "File Generated successfully"; GET …/payroll-processing/outstanding/csv and …/payroll-processing/:ws/csv; files are .xlsx.

- **Processing export failure is silent** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub the CSV GET → 500.
  - Expected: Generating toast disappears; no error shown.

- **Payslip download** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: History → crew → "Payslip Preview" → "Download PDF". → Stub POST /runs/payslip → 500.
  - Expected: "Generating…"; POST /api/v2/payroll/runs/payslip { week_starting, user_id }; PDF downloads. Failure: no toast (console only).

### 09 Payroll › Payroll › Journal ledger

- **Payroll account credit rows** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: With two Payroll Accounts configured, open the ledger.
  - Expected: Group "Payroll Accounts" with one row per code (code name as description); code not editable (title "Set in Payroll Entry Setup → Payroll Accounts"); Credit typed, blank until entered.

- **Description casing** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Format "title", crew Jane Doe, week 22–28 Jun 2026. → Switch to uppercase. → Use a week crossing months.
  - Expected: "Week 22-28 Jun 2026 Jane Doe Basic"; "WEEK 22-28 JUN 2026 JANE DOE BASIC"; cross-month form "29 Jun-5 Jul 2026". Each line uses its own card's week.

- **Journal Grouping** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Turn grouping on. → Open a week with several OT lines for one crew member.
  - Expected: One "Overtime" line per crew member per week (not per journal).

- **Tax line** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Click "Tax" (title "Add a tax line for this timecard") on a timecard (base £1,000.00). → Pick a 20% rate. → Type £50 as the amount. → Type £1,500. → Click "Remove this tax line" and save.
  - Expected: Step 1: tax row with no amount until a rate is picked (saves as 0). Step 2: £200.00. Step 3: rate becomes 5%. Step 4: rate cleared (over 100%). Step 5: row gone, save omits it.

- **Split a line** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: On a £100.00 line click "Split line". → Split again. → Set the first allocation to 70 (with two). → Remove one of two allocations.
  - Expected: £50.00 / £50.00; then £33.33 / £33.34 / £33.33; editing gives £70.00 / £30.00; removing down to one collapses the split. A blank allocation date inherits the parent's.

- **Balance bar** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Debits £1,000.00, type credit £1,000.00. → Credit £900. → Credit £1,100.
  - Expected: "Balanced"; "Non-Balanced" with "Variance" £100.00 (amber); "Non-Balanced" with −£100.00 (red). Split rows count once.

- **Closed-period rows are read-only** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub a lock date after a saved row's effective date.
  - Expected: "Locked — closed periods are read-only"; that row's inputs and "+ Tax" / "Split line" hidden; header date skips it.

- **Post gate: unfixable or nothing to post** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Week with an awaiting_approval card with rows → "Post". → Repeat with an unpaid card. → Week where only payroll-account rows remain.
  - Expected: Alert "Can't post yet": "1 timecard in this ledger isn't locked yet…", "Still awaiting approval — these can't be locked from here:" with "(Awaiting approval)" / "(Unpaid)". Step 3: "Nothing to post".

- **Post gate: incomplete lines** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Clear the code on one postable line, leave a date blank on another. → Click "Post".
  - Expected: Alert "2 lines can't post yet — fill in the missing fields:" with "(nominal code)" / "(effective date)"; rows marked red and groups expanded; Post title "Every line needs a nominal code and an effective date before you can post".

- **Post failure** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub the post → 422.
  - Expected: Server error toast and inline red message; modal stays open.

- **View and edit a posted journal** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: After AH-PAY-105, pick it in "Posted Payrolls". → Edit a line, "Save". → Pick "-".
  - Expected: Posted lines shown; "Post" and "Effective date" hidden; save echoes the entry id and journal number; "-" returns to the working ledger. Picker resets on week change.

- **Fully posted week** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Open a week where every card is posted.
  - Expected: Working view empty (no payroll-account rows), "All timecards in this week are posted.", "Post" hidden.

- **Ledger reads fail silently** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub journal-ledger/fetch and the GET → 500.
  - Expected: Rows show derived defaults (uncoded), no posted journals, no toast.

- **Non-accountant sees a note, not the picker** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: P-VIEW
  - Steps: As P-VIEW open /payroll/run on a week with posted journals → "Journal Ledger".
  - Expected: "N journal(s) already posted for this week — {journals}. Posted lines are no longer editable here." instead of "Posted Payrolls".

### 09 Payroll › Payroll › History

- **Crew detail tabs** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Click a crew member.
  - Expected: GET /api/v2/payroll/timecards/weekly/:id and GET /api/v2/deal-memo/deals/active/:userId; tabs "Pay Code Breakdown", "Payslip Preview", "Audit Trail"; Nominal column read-only (entry code, else deal template, else red dash); footer "Week Gross Total".

- **Audit trail, search, empty states** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: "Audit Trail". → Search "Search name, designation, department…" for "zzz". → Open a week with no paid cards.
  - Expected: History sorted by time; "No history recorded for this timecard." when empty; "No crew match “zzz”."; "No crew on this week yet.".

- **Selection leads nowhere** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Tick paid rows; use "Select all". → Try a posted row.
  - Expected: "N selected" shows but no post action exists; posted rows can't be ticked ("Posted — not selectable").

### 09 Payroll › Payroll › Production-report estimate

- **Roster and selection** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll · account: P-VIEW
  - Steps: As P-VIEW open "Production Report Payroll". → Select a crew member without a timecard, then one with.
  - Expected: Current period; crew with deals listed; GET /api/v2/payroll/weekly/:ws/crew; no timecard → banner "No timecard this week. Use “Fill from report” above…"; with one → their real week.

- **Fill and clear single days** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Click a day's "Fill from report". → Click the filled day. → Fill two days → "Clear all".
  - Expected: Day filled; clicking it ("Filled from Production Report — click to clear") clears it; "Clear all" removes every estimate. Real timecard days are never overwritten.

- **Auto-fill mode** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Click "Auto-fill from production report". → Open another crew member. → Page to next week and back.
  - Expected: aria-pressed="true"; each opened crew member is filled once per week; unopened rows show an "auto" pill; mode survives paging but not a reload.

- **Settled timecards can't be filled** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Select a crew member whose week is locked, paid, unpaid or posted.
  - Expected: No fill buttons.

- **No deal or no report data** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Select a crew member without an active deal. → Pick a week with partial report data. → A week with none.
  - Expected: Banner "No active deal — pay can't be estimated…"; partial "Filled 2 of 5 blank days from the Production Report — 3 have no report data."; none "No Production Report data to fill for the blank days.".

- **Report is scoped per crew member** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Fill crew X (unit A), then crew Y (unit B).
  - Expected: Each request carries that crew member's user_id; X and Y get different call/wrap times.

- **Week change and busy lock** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Start a whole-week fill; try clicking another crew row. → After it ends, change week.
  - Expected: Other rows and fill buttons are blocked while filling; changing week clears every estimate.

### 09 Payroll › Payroll › Realtime & badges

- **Run refreshes on another user's action** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: A-FA and A-PA both open Run on the same week. → A-PA locks a row.
  - Expected: A-FA's row turns "Locked" without reload or skeleton (timecard:locked → ah:payroll:list). Same for Producer Board and History.

- **Outstanding modal follows that crew member** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: A opens the Outstanding modal for crew X. → B marks one of X's weeks paid, then attaches a claim.
  - Expected: A's modal refetches after each (timecard:marked_paid, timecard:claim_added → ah:payroll:outstanding:<userId>).

- **Changes with no broadcast** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: A on Processing "Outstanding". → B sends a week to the run, unlocks a card, saves the ledger.
  - Expected: A's Outstanding list and screens don't change until A reloads or acts. Approver-list edits by B reach A's open Run through production_setup:updated only if the settings save emits it (unverified).

### 09 Payroll › Payroll › Errors, empty & loading states

- **Queue failures and empty weeks** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub GET /weekly/:ws/processing → 500, then empty. → Stub GET /weekly/processing empty.
  - Expected: Run: table "No crew match current filters", no toast. Processing: "No timecards approved for this week yet". First load shows skeleton rows; refetches after actions never swap back to skeletons.

- **Metadata failures** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Stub GET /payroll/metadata → 500 as A-FA. → Stub only GET payroll-settings → 403.
  - Expected: Step 1: A-FA loses Final Approve (PA/FC keep it through designation). Step 2: approval buttons unaffected; ledger falls back to uppercase, grouping off, no payroll-account rows.

### 09 Payroll › Payroll › Theme & windowed view

- **Dark mode** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Toggle the landing theme switch to dark. → Open Run, the ledger, "Post", and Entry Setup "Add / Edit".
  - Expected: Tiles, tables, drawer and the body-portalled Post and Payroll Accounts overlays render dark; no light-only panels.

- **Windowed view** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: Turn on Windowed View. → Film Tools → "Payroll". → Account Hub sidebar → "Payroll" → "Payroll Entry Setup".
  - Expected: Step 2: maximized window "Payroll" with producer tiles. Step 3: accountant tiles and Entry Setup open inside the window (no bounce to the landing).

- **Modal stacking** — planned (e2e/account-hub/09-payroll.md)
  - Where: /film-tools/account-hub/payroll
  - Steps: In a window, open the ledger → "Post". → Trigger the post lock modal.
  - Expected: Post modal and confirm modal sit above the ledger overlay and the window, below dropdown pickers.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Access & navigation

- **Sidebar lists the report pages and hides retired ones** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report · account: ACC
  - Steps: As ACC open Account Hub. → Read the "Reports" and "Management" sections.
  - Expected: "Reports": "Cost Report", "Period Close". "Management" includes "Trial Balance", "Bible Report", "Bank Reconciliation", "Tax Filing". No "Analytics" and no "Journal Ledger" entry.

- **Back to Account Hub** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: On the worksheet click "Back to Account Hub".
  - Expected: Navigates to /film-tools/account-hub, which the hub redirects to Purchase Orders (accountant Queue tab). Same from Trial Balance, Bible Report and Period Close back buttons.

- **Legacy analytics URL redirects** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Visit /film-tools/account-hub/analytics/overview.
  - Expected: Replaced with /film-tools/cost-report/analytics; Analytics page renders.

- **Analytics button and back** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: On the worksheet click "Analytics". → Click the Analytics back button.
  - Expected: Opens /film-tools/cost-report/analytics; back is browser-back (navigate(-1)) and returns to the worksheet.

- **Accountant without cost_report_tool is bounced from Analytics and posted CRs** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report · account: ACC-noCRT
  - Steps: Log in as ACC-noCRT. → Click "Analytics". → Return, open Live CR › "CR History", click a row.
  - Expected: Both land on /film-tools (view gate), although the worksheet itself opens.

- **Hub right revoked while on the worksheet** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: ACC on the worksheet. → Admin removes ACC's account_hub_tool view access.
  - Expected: ACC is redirected to /film-tools without a reload (MDI window closes instead).

- **Worksheet inside the Account Hub MDI window** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open Account Hub as an MDI window. → Navigate to Cost Report.
  - Expected: Worksheet renders inside the window with the same gate; as DEPT the window is closed/redirected.

- **Legacy Cash & Close URL** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Visit /film-tools/account-hub/invoices/cash-close.
  - Expected: Replaced with /film-tools/account-hub/period-close?tab=cash-close; "Cash & Close" tab active.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Viewing & filters (periods, layers, departments, currency)

- **Column layout** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Read the grid header.
  - Expected: Groups "Actuals" (Actuals This Period, Actuals to Date), "Commitments" (PO / Card / Cash / Payroll Commits), "Forecast" (Estimate To Complete, Estimated Final Cost), Budget, "Analysis" (Total Variance, Variance This Period). ETC / EFC / VTP headers carry the editable tint. No horizontal scroll at 1280px (scrollWidth === clientWidth on the scroller).

- **Filters are pending until Compute** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Change "Company". → Observe. → Click "Compute".
  - Expected: After 1 a "Compute" button appears and no request fires; after 3 one GET /live fires with the new filter and the button hides. Same for "Budget", "Currency", "Version".

- **Consolidated vs single company** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Choose "All Companies — Consolidated" → Compute. → Choose one company → Compute.
  - Expected: Request 1 has no company_id; request 2 has company_id=<id>; figures change accordingly.

- **Budget default and placeholder version** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open the worksheet. → Open "Budget".
  - Expected: Preselects LIVE, else APPROVED, else the first budget; GET /live sends budget_version_id=<budget id>. A "v1" placeholder option (no id) appears when no budget uses v1; computing with it omits budget_version_id.

- **Currency switch** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Choose another project currency → Compute.
  - Expected: GET /live with currency=<code>; amounts show that currency's symbol (the server's currency); existing ETC overrides are rescaled by the exr ratio.

- **Week stepper applies at once** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click "Previous week". → Click "Next week". → Click "Jump back to current week".
  - Expected: Each click refetches without Compute; label reads "Wk N · w/e DD Mon …"; "Next week" is disabled on the current week; the jump control shows only when off the current week.

- **Figures and Expand controls** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Toggle "Figures" £0 / £0.0 / £0.00. → Use "Expand" Headers / Nominals / Set Codes and "Collapse all" / "Expand all".
  - Expected: Decimals change on screen only (no request); tree depth expands/collapses as chosen.

- **Filter and sort switch to a flat list** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Choose filter "Over Budget". → Clear it ("Clear ✕"), then click a column header to sort.
  - Expected: Flat header list with chip "Filter: Over Budget" (headers with budget > 0 and negative variance); sort shows "Sort: <col> <dir>"; clearing restores the tree. "Active This Wk" keeps headers with ATP > 0 or ATD > 0.

- **Find by code or name** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Type a known code in "Search code or name…". → Type zzzz. → Press Esc.
  - Expected: After ~200 ms the tree prunes to matches (highlighted, ancestors shown), subtotals/Grand Total unchanged, no request; zzzz shows "No codes or descriptions match “zzzz”."; Esc clears at once.

- **Search overrides filter and sort** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Apply "Over Budget". → Type a search term.
  - Expected: Grid returns to the hierarchical (searched) view while the query is set.

- **Project without COA or budget is gated** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: page.route GET /api/v2/account-hub/budgets → empty list (or use the no-budget project). → Open the worksheet.
  - Expected: Message "Please upload Budget to continue." (or "Chart of Accounts" / "Chart of Accounts and Budget"); both filter rows disabled; no /snapshots or /live call.

- **Non-Allocated and Contractual sections** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Use data with an uncoded/mis-coded line and contractual budget lines. → Open the worksheet.
  - Expected: Red "NON-ALLOCATED ITEMS" (code NA) is last; violet "CONTRACTUAL ITEMS" (code CI) sits above it; sentinel rows show -, mis-coded rows keep their raw code; contractual rows are not clickable, NA rows are.

- **A week with a matching snapshot is frozen** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Step to a week that has a lock or weekly snapshot starting that Monday (same currency). → Change Company → Compute.
  - Expected: GET /snapshots/:id is used instead of /live; Company/Budget changes don't change figures; nothing on screen says "frozen".

- **Live CR pane has independent filters** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click "Live CR". → Change its Currency → Compute. → Switch back to "Cost Report Worksheet".
  - Expected: Only the Live CR grid refetches; the worksheet pane keeps its own filters and figures.

- **Live CR pane is read-only** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: On "Live CR" › "Current CR" click an ETC cell. → Read the top bar.
  - Expected: No input opens; Save, Save Version, History, Lock and Publish are hidden; Overages and Export still show (they describe the worksheet pane).

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Figures & reconciliation checks

- **Card, petty cash and payroll commitments** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Post a card receipt, a cash claim and a payroll run on known codes. → Refresh.
  - Expected: Card Commits, Cash Commits and Payroll Commits rise by the posted amounts; drawer sources CARD / CASH / PR.

- **Variance This Period baseline** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: On a project with no weekly snapshot read VTP. → Publish a Weekly CR. → Next week, change spend and read VTP.
  - Expected: VTP is 0 with no prior weekly post; afterwards VTP = current Total Variance − that line's variance in the newest weekly snapshot ending before the week. Lock and Custom snapshots never act as baseline.

- **Bible Report agrees with worksheet actuals** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Worksheet: note Actuals to Date for code X (consolidated, project default currency). → Bible Report: Account from/to = X, Source = Invoice, Open POs "Excluded", Date Range from project start to today, same currency → Run.
  - Expected: "Total X" equals the worksheet's Actuals to Date for X (confirm with product whether credit notes are inside the Invoice source; see Open questions).

- **Ledger drawer drill-down** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click a nominal code. → Close it ("Close"), click that row's "Actuals This Period" cell. → Click its "PO Commits" cell.
  - Expected: Bottom sheet "Line items" lists Actuals then Commits, with Date, Ref, Type, Description, Supplier / Crew, Code, Amount and "Total:". Requests: GET /account-line-items?code=X&currency=…, then type=actuals, then type=commits&source=po. Header ATD/Commits match the grid row for a consolidated live week.

- **Drawer ignores company and week** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Compute with one company. → Open a code's drawer.
  - Expected: Request has no company_id / period_start / period_end; drawer lists all-company, all-time lines (can differ from the grid row).

- **.direct row for header-coded spend** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Post an amount coded straight to a header code H. → Open the worksheet and the row's drawer.
  - Expected: A first nominal "H.direct — direct entries" shows and rolls into H; the drawer request uses code=H (suffix stripped).

- **Overages badge and modal** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Read the "{n} over" pill on the "Live CR" tab. → Click "Overages".
  - Expected: n = headers where Budget − EFC < 0 (zero-budget headers count); modal lists them with "({amount}) over", "Note for producer…" box and "Go to Live CR". Title reads "Over-Budget Lines — Week 4" whatever the week (K).

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Editing & versions

- **Edit EFC becomes an ETC override** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Type EFC = E above ATD + commits. → Type EFC below ATD + commits.
  - Expected: Case 1: ETC = E − (ATD + commits). Case 2: ETC clamps to 0 and no warning is visible (showToast renders nothing).

- **Typed pence are dropped** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Type ETC 123.45 with Figures £0.00.
  - Expected: Cell shows 123.00 (Math.round).

- **Click-away commits and is swallowed** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open an ETC cell, type a value. → Click the row's account name. → Open another ETC cell from an open one.
  - Expected: Step 2 commits the value and does not expand/open the row; step 3 moves straight to the next editor in one click.

- **Save is enabled only with a selected version** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: With "— Latest (unsaved) —" hover "Save". → Select a saved version, edit, click "Save".
  - Expected: Disabled with title "Pick a saved version to enable Save"; then PATCH /weekly-etc/versions/:id with the lines and currency.

- **Load a version and return to Latest** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Choose a version → Compute. → Choose "— Latest (unsaved) —" → Compute.
  - Expected: GET /weekly-etc/versions/:id, its overrides applied; step 2 clears ETC overrides but keeps VTP overrides.

- **Saved zero reloads as "not set"** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Set ETC 0 on a line, Save Version. → Reload and load that version.
  - Expected: ETC shows the default (remaining budget), not 0.

- **Version loaded after a currency change** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Compute with currency B. → Load a version saved in currency A.
  - Expected: Expected: overrides converted A → B. Code converts with the currency captured when the week's versions loaded (likely wrong figures).

- **Unsaved edits and locked weeks** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Edit ETC, leave the page and return. → Step to a locked week and click an ETC cell.
  - Expected: Edits are gone with no prompt. On a locked week no input opens, yet "Save Version" stays enabled.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Period lock / close / publish

- **Lock button and modal** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: On an unlocked week click "Lock Wk N · …". → Read the modal. → Click "Cancel".
  - Expected: Modal "Lock Cost Report", "Lock through Wk N … — this cannot be undone.", Period / Range / Budget Version rows, optional Note ("Why is this period being locked?"), "Locked periods cannot be reversed". Cancel closes with no request.

- **Lock refused by the server** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: page.route POST /lock-period → 409 (already locked) or the unposted-items error. → Confirm "Lock".
  - Expected: Server message toast; progress toast "Lock failed"; no snapshot posted.

- **Lock succeeds, snapshot fails** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route POST /snapshots → 500 only. → Confirm "Lock".
  - Expected: Error toast; progress toast "Locked …" with "Snapshot post failed: …"; week still shows locked.

- **Summer-time week end in a DST browser** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Playwright timezoneId: 'Europe/London', a week between Wk 13 and Wk 43. 2. Inspect the Lock request and the Save Version week_ending.
  - Expected: Expected as_of on the week's Sunday. Code sends next Monday 00:59:59.999 (fixed-ms weeks), which can lock the following week; the Publish modal's CR Version list can come back empty.

- **Another accountant's lock is not reflected** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: ACC and ACC2 on the same unlocked week. → ACC2 locks it.
  - Expected: ACC's figures silently refresh, but ACC's Lock button stays enabled until reload; a second Lock gets a 409.

- **Publish Weekly while viewing a past week** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Step back two weeks. → Publish cadence "Weekly".
  - Expected: Body carries no period window; the server posts the week containing today (not the week on screen).

- **Publish Custom window** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Cadence "Custom". → Read the default dates; try End after today; set Start/End. → "Publish CR".
  - Expected: Defaults to the last 7 days; both inputs max today, End min = Start ("End can't be later than today — defaults to today."); POST /snapshots {cadence:"adhoc", period_start, period_end}; row chip "Initial". East of UTC the Start box displays the previous day (K).

- **CR Version default; unsaved edits not published** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: With a selected version open Publish. → Repeat with "— Latest (unsaved) —" and on-screen edits.
  - Expected: Case 1 defaults to that version (etc_version_id sent). Case 2 defaults to "None — no ETC overrides" and no etc_version_id; the snapshot opens without those edits.

- **Publish failure** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route POST /snapshots → 500. → Publish.
  - Expected: Error toast from the server message; progress toast "Post failed"; "Publish" re-enabled.

- **CR History list** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: "Live CR" › "CR History". → Use "All" / "Daily" / "Wk-end". → Click "Refresh list".
  - Expected: Header "Posted Cost Reports", "{n} posts · last …"; chips Daily / WK / Lock / Initial; Wk-end shows WK, Initial and Lock rows; Refresh re-calls GET /snapshots; empty filter shows "No posts in this filter."

- **Open a posted snapshot from CR History** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click a CR History row (ACC has cost_report_tool). → Click "Back to CR History".
  - Expected: Opens /film-tools/cost-report/posted/:id; back returns to the worksheet with "Live CR" › "CR History" open.

- **Close Period refused** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route POST /lock-period → 409/422 with a message key. → Click "Close Period".
  - Expected: Red message shows the server text raw (a translation key, not via showApiError).

- **Period Close tabs** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click "Cash & Close". → Click "Publish Package". → Click "Period Close".
  - Expected: URL ?tab=cash-close (lazy Cash & Close page, GET /api/v2/invoices/analytics/cash-close), ?tab=publish ("Publish Closing Package"), then no query.

- **Closing package validation** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: On Publish Package type bad@ in "External emails", press Enter. → Add a valid email, no report. → Tick "Cost Report". → "Add package", remove it.
  - Expected: Invalid email shakes and is not added; "Publish" stays disabled until a package has ≥1 recipient and ≥1 report, then reads "Publish (1)"; packages add and remove (the last one can't be removed). Reports offered: Cost Report, Trial Balance, Bible Report.

- **Publish a closing package** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Package with a test inbox + a team member + all three reports. → Click "Publish (1)".
  - Expected: POST /api/v2/cost-reports/closing-package/publish {packages:[{user_ids, emails, reports}]}; message "Published 1 package. Recipients have been e-mailed."; builder resets to one empty package; email arrives.

- **Closing package failure** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route the publish call → 500. → Publish.
  - Expected: Error message (server text raw or "Failed to publish closing package."); builder keeps its contents.

- **Top-bar History opens an empty panel** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click "History" on the worksheet pane.
  - Expected: Panel "Cost Report History" is empty even when snapshots exist (real history is Live CR › CR History).

- **Published and lock snapshots reach readers** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: ACC publishes a Daily CR and locks a week. → DEPT opens Tools › Cost Report › "Posted CRs".
  - Expected: Both rows appear (Daily chip, Lock chip "Period Lock — …").

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Trial Balance

- **Filters are a draft until Refresh** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Set Account from/to, a Company, another Currency, tick "Include Zero Accounts". → Click "Refresh".
  - Expected: "Refresh" appears only while the draft differs; request carries account_start, account_end, company_id, currency, zero_accounts=true. "All companies" omits company_id (single-select).

- **Date Range mode and no-lock label** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Switch "Period" to "Date Range", set Period from/to → Refresh. → On a project with no lock read the Current Period label.
  - Expected: Epoch-ms period_start/period_end for the chosen days; with no lock the label reads "Till <date>".

- **Empty and error states** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route the TB call → []. → Route it → 500.
  - Expected: "No account balances" / "No data for these filters."; then "Couldn't load trial balance" / "Adjust the filters and Refresh."

- **Export** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click "Export" › "Export PDF", then "Export Excel", "Export CSV". → Route one export → 500.
  - Expected: POST /trial-balance/export/{pdf,xlsx,csv} with the applied filters and English period_label; file trial-balance_YYYY-MM-DD_HHMM.<ext>; a copy goes to the File Cabinet (Cost Report › Exports). Export disabled with no rows. Failed export shows nothing to the user (K).

- **Unbalanced pill** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route TB rows with debit ≠ credit.
  - Expected: Pill reads "Unbalanced".

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Bible Report

- **Filters sent as parameters** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Pick 2 Account Types, 2 Sources, 2 Companies, one Vendor, a Tax type and a Tag. → Run.
  - Expected: account_type, source, company_id, tax, tags as CSV; vendor_id as one id; cleared vendor omits it.

- **Purchase Order source and Open POs toggle** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Source "Purchase Order" only → Run. → Set "Open POs" to "Excluded" → Run.
  - Expected: Step 1 returns no rows (POs aren't in the GL). Step 2 sends include_open_pos=false; "Included" (default) sends nothing.

- **Date Range needs both dates** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Choose "Date Range" with only a start date.
  - Expected: "Run Report" disabled until both dates are set.

- **Partial and total failure** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route /bible with a non-empty errors map. → Route it → 500.
  - Expected: Banner "Some sources failed:" listing the buckets; then "Failed to run Bible Report" (or the error message).

- **Export uses current filters, not the last run** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Run. → Change a filter without running. → "Export" › "Export CSV".
  - Expected: Body carries the changed filter (file won't match the screen); file bible-report_<ts>.csv plus File Cabinet copy; Export disabled while no accounts are loaded.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Analytics

- **Analytics loads and filters apply on Done** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open /film-tools/cost-report/analytics. → Click "Filters", change Currency, Entity / company, Department, Unit. → Click "Done".
  - Expected: GET /api/v2/cost-reports/analytics/modules and GET /analytics/overview (fetched twice on entry); module strip and "Overview"; no refetch until "Done", then one refetch with currency, entity, depts, units (ids). "Reset all" clears chips.

- **Period filter** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: In "Period / date range" choose "Date range", set Start/End → Done. → Choose "All time" → Done.
  - Expected: from/to epoch ms (UTC midnight to end of day); All time sends neither. No "Current period" option.

- **Analytics error and empty** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route overview → 500. → Route it → empty data.
  - Expected: "Couldn’t load analytics" with "Retry" (refetches); then "No analytics yet" / "There’s nothing to show for this selection."

- **Non-accountant with the tool can open Analytics** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report · account: DEPT
  - Steps: Log in as DEPT. → Visit /film-tools/cost-report/analytics.
  - Expected: Page opens (no accountant gate; spec calls Analytics accountant-only).

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Export / print / download

- **Export ignores the company filter** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Compute with one company. → Export.
  - Expected: Body has no company_id (file shows consolidated figures while the screen shows one company).

- **Export from the Live CR pane** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Give Live CR a different week/currency than the worksheet. → Export from Live CR.
  - Expected: Body uses the worksheet pane's applied week/currency, not Live CR's.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Realtime & refresh (two users)

- **Source change raises the pill and auto-refreshes** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: ACC on the worksheet with an ETC edit. → ACC2 fully approves a PO.
  - Expected: Amber "Source data updated · Refresh" pill; ~3.5 s after the last event (15 s max) a silent GET /live, then green "Auto-refreshed" chip; the ETC edit survives.

- **Pill click and hidden tab** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Click the pill as soon as it shows. → Repeat with the browser tab hidden during the event.
  - Expected: Click refreshes immediately; with the tab hidden nothing is fetched until it is visible again, then one refresh.

- **Lock / publish by another accountant** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: ACC on Live CR pane. → ACC2 publishes a CR.
  - Expected: Live CR figures refresh silently (no pill: Live CR ignores the PO event); CR History list does not reload until opened again or refreshed.

- **COA change by another user** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: ACC on the worksheet. → ACC2 adds a COA code in another browser.
  - Expected: Worksheet keeps its cached COA until a reload in a fresh session (session cache); document as known behaviour.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Errors, empty & loading states

- **/live failure** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route GET /live → 500. → Open the worksheet.
  - Expected: Expected: an error message. Code: empty grid with no error (live.error never rendered).

- **Version save failures surface the server message** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Route POST /weekly-etc/versions and PATCH → 4xx with a message key. → Save Version, then Save.
  - Expected: Error toast with the translated server message; modal closes; version list unchanged.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report (accountant side) › Theme & windowed view

- **Dark mode** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Set localStorage['accounthub-theme']='dark'. → Open worksheet, Trial Balance, Bible, Period Close, Analytics.
  - Expected: All readable in dark: sticky cells opaque while scrolling, pills and modals legible; no light-only blocks.

- **Ledger drawer with two cost-report windows** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/cost-report
  - Steps: Open the worksheet MDI window and the Cost Report tool window. → Open a drawer in the second window.
  - Expected: Expected: drawer inside its own window. Code portals into the first .cr-overlay-host in the document.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report tool › Access & navigation

- **Right revoked mid-session** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: DEPT on Current CR. → Admin removes DEPT's cost_report_tool view access.
  - Expected: DEPT lands on /film-tools without reload; in an MDI window the window closes.

- **Legacy and unknown sub-paths** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Visit /film-tools/cost-report/live. → Visit /film-tools/cost-report/foo.
  - Expected: /live is replaced with /current; /foo renders Current CR without rewriting the URL.

- **Tabs are URL-driven** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Click "Posted CRs". → Browser back.
  - Expected: URL /posted then /current; each tab remounts and refetches on return.

- **Hidden Hot Costs deep link** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Visit /film-tools/cost-report/hot.
  - Expected: Static "Hot Costs" placeholder (Draft, "Submit to Studio & Producer", "Not yet submitted", 0 total) with no working buttons; no "Hot Costs" tab in the header; Current CR data requests still fire underneath (K).

- **Opens as an MDI window** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Open the tool in windowed mode.
  - Expected: Maximized window "Cost Report"; the ledger drawer covers only the tool column.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report tool › Viewing & filters (Current CR)

- **Filters apply immediately** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Change "Company" to one company. → Change "Budget". → Change "Currency".
  - Expected: Each change refetches at once (no Compute): company_id, budget_version_id, currency. "All companies" omits company_id.

- **Read-only grid** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Click an ETC cell and look for edit controls.
  - Expected: No input; no Save / Lock / Publish / Export on Current CR.

- **Search codes and names** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Type a code in "Search code or name…". → Type zzzz.
  - Expected: Tree prunes to matches with highlight, totals unchanged, no request; no match message "No codes or descriptions match “zzzz”."

- **Unallocated-costs banner** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: With uncoded spend open Current CR. → Click "View". → Search a term that excludes NA.
  - Expected: Banner "There are unallocated costs at the bottom due to lack of nominal coding."; View expands (only if collapsed) and scrolls to NON-ALLOCATED ITEMS; banner hides while the search prunes NA.

- **Ledger drill-down** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Click a nominal code. → Click a Contractual row.
  - Expected: Bottom sheet over the tool (not over the app sidebar) with Actuals then Commits; contractual rows are not clickable. Drawer request has no company/week, so it shows all-company, all-time lines (K).

- **No COA or budget** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Route GET /budgets → empty list. → Open Current CR.
  - Expected: Panel "Cost report is unavailable" with "Please upload Budget to continue."; no /snapshots or /live call.

- **Failed reload keeps stale figures** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Load Current CR. → Route GET /live → 500. → Change Currency.
  - Expected: Expected: error or empty state. Code: previous figures stay under the new currency label (no error shown).

- **Grand Total project name** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Fresh session, open the tool directly (no worksheet first). → Read the Grand Total row.
  - Expected: Expected "Grand Total — <project>". Code reads the worksheet's module-scope PROJECT, so the name is blank or stale from an earlier worksheet render (K).

- **Currency default follows Production Setup** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: DEPT on Current CR without touching Currency. → Accountant changes the project default currency.
  - Expected: production_setup:updated refreshes settings; the Currency filter switches to the new default and Current CR refetches.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report tool › Figures & reconciliation checks

- **Readers see the default forecast** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: ACC saves a version with ETC overrides and publishes it. → DEPT opens Current CR.
  - Expected: ETC = max(0, Budget − ATD − commits); EFC = max(Budget, ATD + commits); variance 0 until overspend; accountant overrides not shown.

- **Source change refreshes Current CR** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: DEPT on Current CR. → ACC approves a PO.
  - Expected: Pill "Source data updated · Refresh" in the tool header, silent refresh, "Auto-refreshed" chip; PO Commits updated.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report tool › Posted CRs

- **Unread badges** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: ACC publishes a CR. → DEPT checks the Film Tools tile, the "Posted CRs" tab and the row.
  - Expected: Expected unread counts on tile, tab (antd badge, 99+) and row ("{n} new"). Code: nothing writes the cost_report_label badge row, so all read 0 (K).

- **Lock snapshot row** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: After ACC locks a week, open Posted CRs.
  - Expected: Row titled "Period Lock — Wk N…" with a "Lock" chip.

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report tool › Snapshot page

- **KPI tiles vs table** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Open a snapshot published with a CR Version. → Compare the Est. Final Cost tile with the table Grand Total EFC.
  - Expected: Tiles use the server's frozen totals; table shows ETC / VTP as "—" and EFC = ATD + commits, so the two differ when ETC ≠ 0 (K).

- **Export a snapshot** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: "Export" › "Export PDF", then "Export Excel", "Export CSV".
  - Expected: POST /api/v2/cost-reports/snapshots/:id/export/{pdf,xlsx,csv} with project_name, company_name, generated_by = the poster; toast "Exporting PDF…" → "PDF downloaded"; file always named cr-snapshot.<fmt>.

- **Export failure** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Route the export → 500. → Export.
  - Expected: Toast "Export failed" with the raw err.message (or "Export failed — try again."), not showApiError.

- **Deep link and bad id** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Open /film-tools/cost-report/posted/<id> directly. → Open a non-existent id.
  - Expected: Deep link shows "Loading snapshot…" then the page; bad id shows "Couldn't load this snapshot" with the server message (or "Snapshot not found").

- **Back target** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: From Posted CRs open a row, click back. → From the worksheet's CR History open a row, click back.
  - Expected: Case 1 "Back to Posted CRs" → /film-tools/cost-report/posted; case 2 "Back to CR History" → worksheet with Live CR › CR History open.

- **Snapshot laid out on today's COA** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Publish a snapshot. → Delete (or re-code) one of its COA codes. → Reopen the snapshot.
  - Expected: That line moves to NON-ALLOCATED ITEMS; renamed codes show today's name.

- **Poster shown as raw id** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Open a snapshot posted by a user who left the project.
  - Expected: Expected the poster's name. Code shows the raw user id in Posted by, the row and the export's generated_by (K).

### 10 Reports, Bank Reconciliation and Tax Filing › Cost Report tool › Theme & windowed view

- **Theme toggle** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/cost-report
  - Steps: Click the header theme toggle. → Reload.
  - Expected: Tool switches light/dark; choice persists in localStorage['accounthub-theme']; grid, drawer, snapshot page readable in both.

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Access & navigation

- **Department user has no tile** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation · account: DEPT
  - Steps: As DEPT open Account Hub.
  - Expected: No "Bank Reconciliation" item in the sidebar.

- **Tab URLs, unknown path and Open Banking** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click each tab. → Visit /bank-reconciliation/foo. → Open "Open Banking".
  - Expected: Each tab has its own URL; /foo shows Overview; Open Banking shows only "Coming soon — connect bank accounts via PSD2 Open Banking…".

- **Shared lists are fetched once** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open Overview with network logging. → Switch through Exceptions, Fraud Alerts, FX Variances.
  - Expected: On mount: GET /api/v2/bank-reconciliations/periods?sort=created_at&order=desc, GET /api/v2/account-hub/bank-accounts?entity_type=production, GET /exceptions?per_page=200…, GET /fraud-alerts?per_page=200&sort=risk_score…, GET /fx-variances?per_page=200…; tab switches fire none of these again.

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Overview

- **Foreign-currency account KPI** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Make the EUR account's period current. → Repeat with a currency that has no rate.
  - Expected: Bank Balance converted to the project default with note "<amount> EUR converted"; Difference recomputed from the converted figure; Zillit Balance unconverted. No rate → "EUR — no exchange rate set".

- **Current reconciliation card** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Read the progress card. → Click "Continue" (or "Open Workspace").
  - Expected: "<Month> Reconciliation", "In Progress", "Started <date>", Matched / Suggested / Unmatched / Fraud flags legend, "<pct> complete"; buttons go to workspace?period=<id>.

- **Fraud banner** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Current period has fraud flags. → Click "Review".
  - Expected: Banner "<n> Fraud Alerts" / "Requires your review"; Review opens the Fraud Alerts tab.

- **Reconciliation History table** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Read "Reconciliation History". → Click an in-progress row body, then a complete row body.
  - Expected: Columns Bank, Statement ("—"), Period, Txns, Progress, Fraud, Status, Actions; in-progress rows "Open" + bin, complete rows "View". In-progress row click → Workspace for that period; complete row click → History tab.

- **Empty state** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Project with no periods.
  - Expected: "No reconciliation periods found"; no "Open Workspace"; "Import Statement" available.

- **Skeleton flashes on every socket refetch** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: ACC on Overview. → ACC2 matches a line in the same project.
  - Expected: Expected: silent update. Code: the KPI area/table re-shows the skeleton on each ah:bank:* refetch.

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Import / match / exceptions — import

- **Multi-month and signed-off month** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Import the two-month CSV. → Sign off one month, then import another statement for that month.
  - Expected: Step 1 creates one period per month; step 2 creates a new in-progress period beside the complete one.

- **No production bank accounts** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Route GET /api/v2/account-hub/bank-accounts → empty. → Open the import modal.
  - Expected: Shows "No bank accounts found. Add one in Accounts & Rules first." (wrong pointer: accounts are added in Production Setup).

- **File type checks** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Inspect the picker accept. → Drag a .txt file onto the drop zone and import.
  - Expected: accept = .csv .ofx .qfx .qif .mt940 .sta .940 .pdf; a dragged .txt is not rejected client-side and is uploaded and posted (K).

- **Import failure message** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Route POST /import-statement → 4xx with a message key. → Import.
  - Expected: Expected a translated toast. Code shows the raw err.message in the modal (or "Import failed").

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Import / match / exceptions — workspace

- **Workspace tab without a period** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click the "Workspace" tab directly. → Project with no in-progress period.
  - Expected: Picks the first in-progress period (created-desc list); with none shows "No Active Period" / "There are no in-progress reconciliation periods…".

- **Filter pills and counts** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click All / Unmatched / Suggested / Fraud / FX.
  - Expected: Both panels filter; counts come from the response counts; an FX line appears under FX and under its own status.

- **Quick Add on suggested and fraud lines** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Quick Add" on a Suggested line's bar → Add & Match. → On a fraud row with no suggestion use "Quick Add" on the red bar → Add & Match.
  - Expected: Case 1 posts /exceptions/<transactionId>/quick-add; case 2 posts the fraud exception's id and the alert becomes accepted.

- **Effective date vs the cost-report lock** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: With a CR lock, open Quick Entry. → Type a date on or before the lock and Add & Match.
  - Expected: Picker min = lock + 1; a typed earlier date is still submitted (no validation) (K).

- **Dead fraud controls** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click "Review" on an active fraud bar. → Fill Quick Entry "Fraud Flag Details" Reason and Priority, Add & Match.
  - Expected: Review does nothing; Reason and Priority are not in the request body (K).

- **Record a bank transfer** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Quick Entry "Bank Transfer" tile. → Pick From Bank and a same-currency To Bank, Amount → "Record Transfer". → Pick a different-currency To Bank.
  - Expected: POST /api/v2/bank-reconciliations/transfer {from_bank_account_id, to_bank_account_id, amount, …}, then refetch; cross-currency shows "From Amount" and "To Amount" and sends from_amount, to_amount, from_currency, to_currency; Record Transfer disabled until valid.

- **Post an FX variance from the Workspace** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: On a teal FX bubble (Invoice (CUR), Budget Rate, Bank Rate, FX Gain/Loss) click "Post ›". → Set Nominal Code, Cost Centre → "Post Variance".
  - Expected: FX form with Booked at Budget / Actually Paid; POST /fx-variances/<id>/post {nominal_code, cost_centre} with no rates; bubble shows Posted.

- **Unreconcile a fraud-matched line** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Unreconcile a row matched via a fraud acceptance.
  - Expected: Danger confirm "This transaction was flagged for fraud — its fraud alert will re-open."; row returns to the red fraud band.

- **Unreconcile blocked on a signed-off period** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open a complete period by URL (?period=<id>).
  - Expected: Unreconcile disabled with title "A signed-off period can't be unreconciled".

- **Re-run Auto-Match** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Unmatch a line. → Click "Re-run Auto-Match".
  - Expected: Overlay "Re-running auto-match..."; POST /rerun-match/<periodId> then GET /workspace-data; panels replaced (the undone match can come back: backend gap).

- **Expanded view and Quick Entry panel** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Toggle "Expand workspace" / "Exit expanded view". → Press Esc. → "Hide Quick Entry panel" / "Show Quick Entry panel". → Repeat inside the MDI window.
  - Expected: Overlay opens/closes; Esc collapses; back button "Back to Bank Reconciliation"; panel unmounts/remounts and "Add & Match" isn't clipped; inside the window the overlay stays within the window.

- **Currency per row** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open the EUR account's period and a GBP period with FX lines.
  - Expected: Bank amounts use the account currency; an FX row's base amount isn't shown with the foreign symbol; the foreign leg shows in its own currency; ledger amounts and panel totals in the project default.

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Import / match / exceptions — Exceptions tab

- **Groups and period filter** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open "Exceptions" with two open periods. → Use "Filter by reconciliation period".
  - Expected: Defaults to the newest open month; groups "Not In Zillit" (count), "€ FX Variations", "Resolved"; filter shows only with 2+ open periods, offering each month and "All Open Periods".

- **Ignore** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Ignore" on an open debit exception.
  - Expected: PATCH /exceptions/<id>/status {status:"ignored"}; card moves to "Resolved" tagged "Ignored".

- **Investigate a credit line** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Investigate" on a credit exception. → "Investigated".
  - Expected: Status "Under Investigation" (under_investigation), then investigated ("Investigated").

- **FX payment jumps to FX tab** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "View FX" on an FX exception.
  - Expected: FX Variances tab opens.

- **Quick Add from Exceptions** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Quick Add" on an open debit exception. → Fill and click "Add Entry".
  - Expected: Modal "Quick Add to Zillit Ledger" with Date, Invoice Number, Description prefilled with the type hint (e.g. "Monthly service charge. No matching entry in Zillit."), Effective Date (min lock + 1), Amount, Tax, Nominal Code, Cost Centre; POST /exceptions/<id>/quick-add {date, invoiceNumber, description, effectiveDate, amount, vatType, vatRate, nominal, costCentre}; modal closes without waiting (no error feedback if it fails).

- **Export and empty states** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: With "All Open Periods" hover "Export PDF". → Pick one period → "Export PDF". → Project with no open period.
  - Expected: Disabled with "Select a single period to export"; then POST /exceptions/export-pdf download plus a File Cabinet copy; no period → "No Active Period" / "Import a bank statement to start a reconciliation period."; none → "No exceptions detected. All transactions are matched or flagged."

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Fraud alerts

- **Alert cards** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open "Fraud Alerts".
  - Expected: "<n> Active" badge, "Audit Log" button; cards sorted by risk with "RISK" score, "High Risk" (≥ 75) or "Medium Risk", type label, signals, "Suggested Match", "Registered Vendor:" / "<name> is not in the approved supplier register.". No alerts → "No fraud alerts detected." plus "What Zillit checks automatically".

- **Investigated — No Issue** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click "Investigated — No Issue" on an active alert.
  - Expected: POST /fraud-alerts/<id>/dismiss; badge "Dismissed" (wire dismissed); buttons gone.

- **Escalate to Finance** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Escalate to Finance" on an active alert. → Look at the escalated card.
  - Expected: POST /fraud-alerts/<id>/escalate; badge "Escalated"; "Investigated — No Issue" still offered, Escalate gone.

- **Audit log and export** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click "Audit Log". → Filter by bank, period, user, type. → "Export" › "Export CSV", then "Export PDF".
  - Expected: GET /fraud-alerts/audit-logs?per_page=200; "<n> of <m> entries"; columns Date / Time, Bank, Account No., Action, By, Fraud Type, Risk, Period, Description, Amount…; exports POST /fraud-alerts/audit-logs/export-csv / export-pdf of the filtered rows → audit-log.csv / audit-log.pdf plus File Cabinet copy; empty "No audit log entries yet."

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › FX variances

- **FX table and rates** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open "FX Variances".
  - Expected: Columns Date, Supplier, Reference, Curr., Foreign Amt, Budget Rate, Bank Rate, Variance, Status (Posted / Unposted); budget rate from Production Setup › Project Currencies, missing shows "Not set in Production Setup → Project Currencies — enter it when posting"; "Net FX Gain"/"Net FX Loss" summary.

- **Post All** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: With a row missing a rate read "Post All Variances". → Fix rates, click it.
  - Expected: Disabled with "Rates missing:" note while any unposted row lacks a rate, or when all are posted; posts each row with POST /fx-variances/<id>/post (not post-all); message "Posted <ok> of <total>" (or "— <n> failed"). Posted rows keep their posted rate later.

- **Dead FX controls** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click "Export to Cost Report". → Read the "Journal Preview — Post All Variances".
  - Expected: Export does nothing; preview uses nominals 7850 and 1200 whatever is posted (K).

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › History, delete and export

- **History table** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open "History". → Switch the bank account selector.
  - Expected: Defaults to the first account; columns Month, Account, Opening, Closing, Txns, Fraud, FX Var, Status, Signed by, Date, Actions; balances in the account currency.

- **View a signed-off period** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "View" on a complete period.
  - Expected: "Period Details" modal with the read-only summary (Bank Transactions, Ledger Entries of paid lines).

- **Bulk delete; signed-off excluded** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Tick two open periods (or "Select all open periods"). → Click "Delete 2 periods" → confirm. → Look at a complete row.
  - Expected: One POST /periods/bulk-delete with both ids; while selecting, row clicks toggle selection; "Clear" resets; complete periods have no checkbox or bin.

- **Export reconciliation PDF** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Export PDF" on Overview or History. → "Select All (<n>)" → "Export".
  - Expected: Modal "Export Reconciliation PDF" lists complete periods only ("<n> txns · Signed <date>"); POST /periods/export-pdf {period_ids, company details} download plus File Cabinet copy; none → "No completed periods available for export."

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Sign-off

- **Open exceptions counted across all periods** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Period A clean; period B has an open exception. → Sign off A.
  - Expected: Expected "ready". Code requires a note because the open-exception count includes period B (K).

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Accounts & Rules

- **Bank accounts are read-only** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open "Accounts & Rules".
  - Expected: "Connected Bank Accounts" table (Bank Name, Account Holder, Sort / Account, IBAN, Nominal) with no edit controls; empty → "No bank accounts yet. Add one from Production Setup → Bank Accounts."

- **Save auto-match rules** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Toggle "Amount + supplier name (fuzzy)". → Click the card's "Save".
  - Expected: PUT /rules-settings with the auto-match rules; button "Saving..." then back.

- **Save fraud thresholds; other card loses edits** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Change "Split payment threshold" amount (unsaved) in the fraud card. → Toggle a rule in the auto-match card and Save it.
  - Expected: PUT /rules-settings; after the br:rules:updated refetch the fraud card's unsaved change is gone (K).

- **Rules load failure** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Route GET /rules-settings → 500.
  - Expected: Expected an error. Code shows DEFAULT_SETTINGS as if saved (K).

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Guarantor portal (accountant side)

- **Portal tab, preview and links table** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open "Guarantor Portal". → Change the period picker.
  - Expected: Intro text; picker defaults to the newest period (may be complete); preview from GET /portal-links/preview?period_id=&bank_account_id= shows every section; "Active Portal Links" table or "No portal links generated yet. Click “Generate & Share Link” to create one."

- **Generate modal period labels** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Open the Period dropdown with complete periods present.
  - Expected: Expected "(complete)" on signed-off periods. Code shows "(in progress)" on every period (tests signed_off) (K).

- **Copy link** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Click "Copy link" on an active row.
  - Expected: Clipboard holds <current origin>/guarantor-portal/<token>; button briefly shows copied state.

- **Edit and re-share** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: "Edit" an active link with 30-day expiry. → "Save Changes". → "Re-share" a revoked link.
  - Expected: "Edit Portal Link" prefilled but Link Expiry resets to 7 days (K); PATCH /portal-links/<id> with the full form; Re-share opens the same modal and PATCHes (resulting status: see Open questions).

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Guarantor portal (public)

- **OTP errors** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Route request-otp → br_portal_otp_email_mismatch. → Route verify-otp → …_invalid, …_expired, …_used.
  - Expected: "No match." on step 1; "That code isn't right.", "Code expired." (Resend), "Code already used." on step 2.

- **Session reuse and expiry** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: After verifying, reload. → Route the view → br_portal_otp_session_expired. → Re-verify.
  - Expected: Reload skips verification; expiry returns to step 1 with "Session expired." over the page; after re-verify the data refetches (no "Link Unavailable").

- **Permissions and redaction** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: Route the view with permissions missing. → Open a link that includes Fraud Alerts.
  - Expected: Missing key hides fraud alerts and transaction detail (fails closed); bank account cell shows name and holder/currency, never sort code or account number; fraud alerts show title and first 150 chars of description (nothing redacted, despite "names redacted by default").

### 10 Reports, Bank Reconciliation and Tax Filing › Bank Reconciliation › Realtime & refresh (two users)

- **Other users' actions refresh the module** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: ACC and ACC2 on the same period. → ACC2 imports, matches and views a guarantor link (guarantor opens it).
  - Expected: ACC's lists and Workspace rows refresh (br:* → ah:bank:*); Guarantor Portal "Views" increments live.

- **Tab badges** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/bank-reconciliation
  - Steps: ACC2 triggers bank-rec notifications. → ACC opens the badged tab.
  - Expected: Tab chips show unread counts; opening the tab clears its chip (emitBankTabRead); the sidebar tile shows the unit total for accountants only; Open Banking has no badge.

### 10 Reports, Bank Reconciliation and Tax Filing › Tax Filing › Access & navigation

- **Non-accountant is redirected** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: As DEPT visit /film-tools/account-hub/tax-filing and /tax-filing/GB/mtd-vat.
  - Expected: Both redirect to /film-tools; no "Tax Filing" sidebar item.

- **Back out of the hub** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Click the back chip, then repeat with the "Management" breadcrumb.
  - Expected: Both go to /film-tools/account-hub (lands on Purchase Orders).

### 10 Reports, Bank Reconciliation and Tax Filing › Tax Filing › Catalogue (dev)

- **Catalogue cards** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Open the hub on dev.
  - Expected: GET /api/v2/tax-filing/catalog and GET /api/v2/tax-filing/registrations/companies; skeleton then cards (flag, country, title, regime, "Open"); "Your countries" / "Other countries" headings only when both groups exist; a card opens /tax-filing/<country>/<key>.

- **Empty catalogue and failures** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Route catalog → []. → Route catalog → 500. → Route companies → 500.
  - Expected: "No tax filings available" + hint; API error toast; companies failure still shows the catalogue (no grouping).

- **Unknown filing** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Visit /film-tools/account-hub/tax-filing/FR/vat.
  - Expected: "This tax filing isn’t available yet." with "Back to Tax Filing" (→ hub).

### 10 Reports, Bank Reconciliation and Tax Filing › Tax Filing › Registrations

- **MTD VAT list and empty state** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Open the "MTD VAT" card with no registrations.
  - Expected: Title "MTD VAT", "Register VAT numbers, connect to HMRC, and file Making Tax Digital VAT returns per company."; GET /registrations/companies + GET /registrations; empty view with "Register a company" and steps "Register VRN", "Connect to HMRC", "File VAT return".

- **Non-UK company can be registered for UK VAT** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Register a company whose country is not GB.
  - Expected: Company is listed "(XX)" and the request still sends country_code:"GB" (no front-end check).

- **Remove a registration** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: "Remove" on a card. → Read the confirm, click "Remove registration". → Register the same company again and open its return.
  - Expected: Modal "Remove this registration?" ("…disconnects it from HMRC and clears its obligation and return history. The box mapping is kept. This can’t be undone."); DELETE /registrations/<id>; pill "Removed <company>"; re-registered company shows the old box mapping.

- **Export registration data** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: "Export data" on a card.
  - Expected: GET /registrations/<id>/export; JSON file tax-filing-<company>-<vrn>.json; pill "Exported <company> data".

### 10 Reports, Bank Reconciliation and Tax Filing › Tax Filing › HMRC connection

- **Callback results** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Visit …/GB/mtd-vat?connected=1. → Visit …/GB/mtd-vat?connected=0&error=access_denied.
  - Expected: Success toast and query removed; failure shows an error toast with the access_denied key (raw unless translated) and query removed, no reload.

- **No connect URL** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Route the authorize call → {data:{}}. → Click "Connect to HMRC".
  - Expected: Error toast with key no_connect_url; button re-enabled; no redirect.

### 10 Reports, Bank Reconciliation and Tax Filing › Tax Filing › VAT return

- **Open the VAT return** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: "Open VAT return" on a connected card. → Reload the browser.
  - Expected: Breadcrumb "Tax Filing / MTD VAT / <company>", URL unchanged; loads GET …/GB/registrations/<id>/obligations, GET …/returns, GET /box-map?companyId=<id>, GET /api/v2/account-hub/tracking-sets; reload returns to the list (drill-in is state).

- **Pick an open obligation** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Choose an open period.
  - Expected: "Obligation" pill "Open", "Connected to HMRC"; "Box mapping" accordion (Box 1 open) and "Return summary" rail with "—" values; Boxes 3 and 5 marked "Auto". Changing period later clears any calculated figures.

- **Fulfilled obligation** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Choose a fulfilled period (filed from Zillit, and one filed elsewhere).
  - Expected: "Filed return" panel, pill "Fulfilled": nine boxes and "Submitted · receipt <ref> · <date>" for a Zillit filing; otherwise the "already fulfilled with HMRC" note; no mapping or submit controls.

- **Map and save boxes** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Box 1: add a code (unknown code opens the add-to-chart dialog), a layer, a tag, a date range. → Box 6: tick "Mark zero — force this box to 0 (ignore the ledger)". → "Save mapping".
  - Expected: Box without codes reads "Not mapped — no ledger codes"; mark-zero greys inputs but keeps them; "Expand all" / "Collapse all" work; PUT /api/v2/tax-filing/box-map?companyId=<id> {rows:[{box, codes, layers, tags, date_from, date_to, mark_zero}]} (only non-empty rows); pill "Box mapping saved for <company>".

- **All boxes zero** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Map codes with no activity, Calculate.
  - Expected: Info toast "All boxes are £0. Ledger rows for this company in the obligation period: <n>. …" (plus GL rows with no company when present).

- **Export the ledger lines** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: "Export ledger (.xlsx)". → Repeat for a period with no rows.
  - Expected: POST …/returns/ledger-export {periodKey}; file vat-ledger_<start>_<end>_<YYYY-MM-DD_HHMM>.xlsx with columns Box, Account code, Debit, Credit, Period, Tracking, Memo, plus a File Cabinet copy; no rows → info "No ledger rows to export for this period."

- **Re-sync failure after submit** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Route POST …/obligations/sync → 500 only after submit. → Submit.
  - Expected: Expected an error or Fulfilled state. Code: period stays "Open" with no draft and no message; a second submit is possible (K).

### 10 Reports, Bank Reconciliation and Tax Filing › Tax Filing › Theme & windowed view

- **Dark mode** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Set localStorage['accounthub-theme']='dark'. → Open hub, MTD VAT list, a return and the submit modal.
  - Expected: Header solid (no blur) in dark; text readable; known light literals: AutoRow / SummaryRail white backgrounds, the toast pill (K). No theme toggle on this route.

- **MDI window** — planned (e2e/account-hub/10-reports-bank-reconciliation-tax.md)
  - Where: /film-tools/account-hub/tax-filing
  - Steps: Open Account Hub as a window and go to Tax Filing.
  - Expected: Hub and filing screens render inside the window; the accountant gate applies; Connect still leaves the SPA (whole page).

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Access & navigation

- **Section switcher jumps to the section's first page** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Click "Artistes" in the segmented control. → Click "Reports".
  - Expected: "Artistes" shows groups "Schedule", "Artistes", "Shoot Day" and navigates to /live-day (first visible item); "Reports" navigates to /wrap-report.

- **Every slug deep-links to its page** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Visit /film-tools/ad-dashboard/<slug> for: todays-list, add-extras, attendance, additionals, submit-day, budget, schedule, live-day, register, onboarding, agency, queries, config, po-queue, wrap-report, ad-report, exhibit-g, templates. → Visit /film-tools/ad-dashboard/foo.
  - Expected: Each page renders; content wrapper [data-screen-label] = list, add, attendance, additionals, submit, budget, schedule, liveday, register, onboarding, agency, queries, config, poqueue, wrapreport, adreport, exhibitg, templates. /ad-report lights "Wrap Report". Budget shows "Budget — Coming Soon". /foo shows a placeholder "Coming up" with pill "This screen follows the same system".

- **Header chrome per tab** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open Today's List. → Open Artiste Register, Shoot Schedules, Wrap Report.
  - Expected: Shoot Day tabs show breadcrumb "Shoot Day / Today's List", pill "Day {n}" (GET /api/v2/ad-shoot-days/day-number?shoot_date=) and the phase tracker "Arrival & Lists", "Attendance", "Additionals", "Submit & PO". Register, Schedule and report tabs show no Day pill and no phase tracker. Back chevron (aria-label "Back to Film Tools") goes to /film-tools.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Rights

- **Queries are exempt from read-only** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: As viewer open Artiste Queries. → Reply to and "Mark resolved" an active query.
  - Expected: POST /api/v2/artiste-queries/:id/reply and /resolve succeed; UI updates as for the AD.

- **Agency tab ignores posting rights** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: As viewer who is an accountant (or unset isAccountant) open Agency. → Edit an agency and save. → Click "Add Agency".
  - Expected: Edit saves (PATCH /api/v2/ad-agencies/:id). "Add Agency" navigates to /config/vendor, which redirects to Today's List.

- **Artiste form reachable by URL for a viewer (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard · account: viewer
  - Steps: As viewer open /film-tools/ad-dashboard/register/new. → Toggle "External artiste", fill first/last name and Category, click "Save & add to list". → Repeat at /register/<id>/edit.
  - Expected: Rule: mutating controls are hidden for non-posting users. Code renders OnboardingForm with no access check, so POST/PATCH /api/v2/artistes is sent. Record the server response.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Today's List

- **Search and filters** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Type part of a name in "Search name…". → Pick a category; Engagement "Direct / PAYE", then "Agency"; Status "Present", "Pending", "Issues".
  - Expected: Rows filter accordingly; "Pending" hides present and blocked rows; "Issues" shows blocked only. "Export" and "Day Report & Vouchers" stay visible even when filters empty the table.

- **Artiste modal** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Click an unmarked row. → Click "Confirm present". → Click a blocked row.
  - Expected: Modal title = name, sub "ref · category", fields Artiste Ref, Category, Engagement, Union, Call / Arrival ("Not arrived"), Wrap, ID status. After step 2 the footer shows a "Present" pill and the same PATCH as AH-AD-017 (call_time on the artiste's shoot day). Blocked row shows "Resolve {reason}", which does nothing; "Close" closes.

- **Send signing link from the voucher** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: On a submitted day open an external Direct PAYE artiste's voucher. → Click "Send signing link". → Open an internal artiste's voucher.
  - Expected: POST /api/v2/supporting-artist-days/:id/send-approval-link; toast "Signing link sent to {name}"; artiste receives the e-mail. The button is absent for internal or agency artistes and on unsubmitted days.

- **Export the list** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Click "Export" → "Export PDF", then "Export Excel", "Export CSV". → Repeat with a 500.
  - Expected: Trigger reads "Exporting…"; POST /api/v2/supporting-artist-days/export {format:'pdf'/'excel'/'csv', rows:<visible rows>, meta:{date, dayNumber}}; download todays-list-YYYYMMDD-HHmm.pdf/.xlsx/.csv. Error → server message toast. No Export button when the day has no rows.

- **Day Report & Vouchers summary** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Click "Day Report & Vouchers".
  - Expected: Modal "Day Report & Vouchers", "Shoot Day {n} · {date}"; tiles Present, Signed {x}/{y}, Issues; list "Voucher lines — confirmed present" with "Signed"/"Unsigned"; "Confirmed voucher total" per currency. "Download PDF" does nothing; "Signed" also counts submitted rows (confirm with product).

- **Header "Submit Day" shortcut** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Wrap every present artiste (phase 4 active). → Submit the day while one artiste is absent.
  - Expected: "Submit Day" appears in the header only once every present artiste is wrapped and opens /submit-day. After submit it is expected to disappear; code keeps it while any row is absent/blocked/draft/signed (phase 4 never "done").

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Add Extras and quick add

- **Today / Tomorrow selector** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open Add Extras. → Click the "Tomorrow" card.
  - Expected: "Which shoot day is this for?" with cards "Today"/"Tomorrow" and UTC dates (e.g. "Tue, 17 Nov"). List title "Today's list"/"Tomorrow's list"; empty text "No extras on tomorrow's list yet."; panel "Add to tomorrow" (skeleton while the day status loads); "Import extras" goes to Artistes Onboarding.

- **Adding to tomorrow stamps tomorrow** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Select "Tomorrow". → Create an artiste with Call Time 06:00.
  - Expected: Button "Create & Add to Tomorrow" ("Add to Tomorrow’s List" for existing); shoot_date = today's UTC midnight + 86 400 000; call_time = tomorrow 06:00Z; toast ends "to tomorrow".

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Attendance

- **Absent and undo** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Click "Mark absent". → Click the red X ("Absent — tap to undo").
  - Expected: PATCH {attendance_status:'absent', call_time:null}, toast "{name} marked absent"; undo PATCH {attendance_status:null, call_time:null}, toast "{name} unmarked".

- **Mark all present** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: With 3 unmarked rows click "Mark all present" (hint "3 not marked"). → Repeat via chevron "Enter one time for all" at 08:00 with one PATCH routed to 500.
  - Expected: Three PATCHes, one list refetch, toast "3 marked present". With the failure: toast "1 of 3 couldn't be marked — reverted."; only that row reverts.

- **Day type per artiste** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Pick "SWD" in "Day type…". → Pick "Other…" and type "Night". → Clear it.
  - Expected: Options come from the artiste's deal day_types. PATCH {day_type:'SWD'}, toast "{name} — day type SWD"; clearing sends {day_type:null}, toast "{name} — day type cleared".

- **No time out here; locked day** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Look for a time-out control on Attendance. → Open Attendance for a submitted day.
  - Expected: No Mark out control on this tab. On a submitted day rows show state only: no buttons, no bulk bar.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Run Current Day and meals

- **Running meal, reopen, remove, unsaved draft** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: "Start meal" then "End meal", then "Reopen". → Add two meals, remove the first ("Remove meal"). → Add a meal, don't save, click "Meals done — Wrap", come back.
  - Expected: Meal types offered: NDM, Breakfast, Lunch, Dinner, Second Meal. Remove renumbers the remaining meal to order 1. The unsaved meal is gone after step 3 (no PATCH); saved meals survive Mid-Day ↔ Wrap.

- **Per-artiste meals** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Under "Artiste Meals" expand an artiste, add a meal, "Save meals".
  - Expected: PATCH /api/v2/supporting-artist-days/:id {meals:[{order,type,meal_start_time,meal_end_time,meal_penalty}]} silently; row summary "1 meal" / "1 logged".

- **Submit gate; Additionals optional** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Click Submit before everyone is wrapped. → Wrap all and click Submit.
  - Expected: Submit stays locked until every present artiste is wrapped; Additionals opens with Mid-Day and never shows "Done"; Submit renders the Submit Day content and the header gets "Submit Day & Post PO".

- **Clearing a recorded time (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Wrap → on a wrapped row clear the out-time chip, click "Save". → Attendance step → clear an arrival chip, "Save".
  - Expected: Expected: the time is cleared. Code calls onOut('')/onPresent(''), which falls back to "now" and re-stamps the current time; WrapRow never calls onClearOut, so no clear control exists.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Shoot Schedules and opened days

- **Schedule list** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Have days today, tomorrow and last week. → Open Shoot Schedules.
  - Expected: Today's row first, then "Upcoming" (ascending) and "Past" (newest first). Row: "SD n" (rank by date, not the server day number), title (episode · scene or "Shoot Day n"), date and "Call HH:MM · Wrap HH:MM", "{n} SAs · {n} present", status pill (Draft, In progress, Wrapped, Submitted, Approved, Published). No days: "No shoot days yet. A shoot day is created when the first extra is added to a date."

- **"View report" day link** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: On a published row click "View report". → Repeat for a day with both a Wrap and an AD report. → Click Back in the editor.
  - Expected: /wrap-report?day=<sd>: one report opens directly; two show the list pinned "Shoot day · {date}" with "Show every day"; none opens the day's Exhibit G. Back returns to Shoot Schedules.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Additionals

- **A tile click never saves the custom draft** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Type custom amount 40 (don't save). → Tick a fee tile.
  - Expected: PATCH additional_fees contains only the saved custom line (or none), not 40; the draft stays in the inputs.

- **Notes, empty deal, locked day** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Type a note and blur; blur again unchanged. → Expand an artiste with an empty deal. → Open on a submitted day.
  - Expected: One PATCH {notes} only when changed. Empty deal: "No allowances or additional fees in this artiste's deal — configure the union / non-union rates and save the artiste first." Submitted: banner "This day is submitted — additionals are read-only. …", inputs disabled. "Expand all"/"Collapse all" and search work.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Submit Day and purchase orders

- **Retry after reload (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard · account: AH-AD-057 step 2
  - Steps: As AH-AD-057 step → 2. Reload the page. → Submit.
  - Expected: Expected: no duplicate. Code keeps created POs in a per-mount ref, so the first agency's PO is created a second time.

- **Urgent routing** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Pick "Purchase Orders — urgent flag". → Submit.
  - Expected: Each PO's notes start "Urgent — "; submit body po_post_type:'urgent'; after submit the select is disabled showing the urgent option.

- **Post a skipped agency's PO later** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: After submit, link Agency B's artistes to a registered agency (edit each artiste). → Reopen Submit Day; click "Post PO".
  - Expected: Card shows "Post PO" (posting users only) → POST /purchase-orders, then PATCH /api/v2/ad-shoot-days/today {purchase_orders:[…existing, new], shoot_date}; toast "PO posted — Agency B (GBP)"; chip "Posted".

- **Artiste Signing Links card** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Before submit click "Send link". → Submit; click "Send link".
  - Expected: Card lists present external Direct PAYE artistes with amounts; external draft ones show "Link sent when ID verified"; none → "No external direct PAYE artistes present." Step 1 toasts "Signing links are emailed automatically when the day is submitted." with no request. Step 2: POST /send-approval-link, toast "Approval link sent to {name}".

- **Unwrapped submit and PO suffix (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: On the Submit Day tab submit with a present artiste who has no wrap time. → Order agencies so the vendor-less group is first; submit.
  - Expected: 1: submit is allowed (no wrap check on this tab; confirm with product). 2: card reference suffix (counted over all groups) differs from the created PO's description suffix (counted over vendor-linked groups).

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › PO / Payroll Queue

- **Queue lists** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open PO / Payroll Queue before and after submitting a day. → Click "View PO". → Submit a day in another tab.
  - Expected: GET /api/v2/ad-shoot-days/po-queue. "Agency POs" cards: reference, agency · SDn · amount · "{name} approved", status badge; "Direct PAYE — payroll queue"; "Pending payroll items" ("Blocked"/"On hold"). Empty: "No agency POs yet — raised when a day with agency artistes is submitted.", "No direct-PAYE payroll runs yet.", "Nothing on hold." Amounts round to whole units (155.50 GBP → "£156"). "View PO" does nothing. Step 3 refreshes the list silently.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Artiste Register and artiste page

- **Register list and filters** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open Artiste Register. → Search a name, a ref, an agency name. → Engagement "Agency"; union name; "Non-union"; onboarding "Verified".
  - Expected: Columns Name, Category, Engagement, Union, Onboarding; union options come from the data plus "Non-union"; name/ref search works, agency search finds nothing although the placeholder reads "Search name, code, agency…"; empty "No matches"/"No artistes match your search or filters."; no data "No artistes yet". Rows with unread show "{n} unread update(s)".

- **Delete an artiste** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: "Delete" on a pending artiste → confirm. → Check a verified artiste.
  - Expected: Confirm "Delete artiste" / Delete "{name}"? This can't be undone.; DELETE /api/v2/artistes/:id; toast "Artiste deleted"; their rows leave Today's List after refetch. Verified artistes have no Delete.

- **Copy portal link** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Click "Copy portal link". → Repeat with clipboard permission denied.
  - Expected: POST /api/v2/artistes/:id/portal-link; clipboard holds <origin>/extras-portal/<token>; toast "Portal link copied to clipboard". Denied write → the toast shows the URL instead.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Manual entry form

- **Crew picker and e-mail** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open "Artiste Name" (External off). → External on, email "x@y". → Save an external artiste with no email.
  - Expected: Picker lists only Stand-Ins and Stunts department users and Supporting Cast designation users; empty "No Supporting Cast, Stand-In or Stunts users in project". "x@y" → "Enter a valid email address.". Blank email saves (the artiste can never pass the portal code check).

- **Documents and new agency** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: "Add document": title "Proof of address", attach a file. → Engagement "Agency" → "Add a new agency" → fill and save the vendor form. → Save.
  - Expected: File uploads to project storage before the POST; additional_documents:[{label, description, attachment}] (attachment optional in AD mode). Agency overlay POSTs /api/v2/ad-rate-config/vendors, closes and selects the new agency with the artiste form intact. Date of birth rejects future dates.

- **Edit a draft** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open a draft artiste → "Edit details". → "Save". → Reopen → "Add artiste". → Edit a pending artiste.
  - Expected: Header "Edit artiste". Save → PATCH /artistes/:id without status, toast "Artiste updated". "Add artiste" → PATCH with status:'pending', toast "Artiste added to the register". Pending edit shows "Save changes". "Clear form" exists in create mode only (toast "Form cleared").

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Agencies

- **Agency list and tabs** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard · account: non-accountant
  - Steps: Open Agency as accountant. → As non-accountant. → Search by contact, e-mail, tax number.
  - Expected: GET /api/v2/ad-agencies. Accountant tabs "All Vendors", "Verified", "Non-Verified" with counts; non-accountant "All", "Verified", "Non-Verified", "Added by Me". Search matches name, contact, e-mail, tax number, department; empty "No vendors match your filter."

- **Edit, verify, delete, history** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: As non-accountant creator edit own agency; check another user's. → As accountant "Mark Verified", delete, open "Vendor History".
  - Expected: Edit/Delete only for accountant or creator; verify accountant-only (POST /ad-agencies/:id/verify); delete confirm "Delete Vendor" → DELETE /ad-agencies/:id; history GET /ad-agencies/:id/history.

- **Another user's agency change (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: User A sits on Agency. → User B adds an agency via Rates & Config.
  - Expected: Expected: A's list updates. Code refetches only on ah:vendor:list, while agency writes fire ah:ad_rate_config, so A likely sees no change until reopening.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Rates & Config

- **Page and currency banner** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open Rates & Config in a project without a default currency, then with one.
  - Expected: GET /api/v2/ad-rate-config; sections "Permissions & Distribution", "Voucher Fields", "Registered Agencies", "Non-union Rates", "Union Pay Breakdown". "Project currency not set. …" only without a currency. Header "Save changes" disabled until an edit.

- **Distribution list** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: "Add recipients" → search → tick two users → "Done". → "Remove recipient" one. → Type in "Agreement text on voucher". → "Save changes".
  - Expected: Summary "{n} selected · e-mailed when a report is published"; empty "No recipients yet — posted reports aren’t e-mailed by default." PATCH /api/v2/ad-rate-config with non_union.distribution:[{kind:'user', ref, name, email}]; toast "Rate config saved"; agreement text not in the payload.

- **Remove a registered agency** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Click "Remove agency" → confirm. → Click "Add".
  - Expected: Confirm "Remove agency?" / "Remove “{name}” from this project's agencies? This permanently deletes the vendor record."; DELETE /api/v2/ad-rate-config/vendors/:id; toast "Agency removed". "Add" opens /config/vendor.

- **Union pay breakdown** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: "Add Union Pay Breakdown" → Territory → Agreement. → Set one allowance (agreement 20.00) to 25 and another (agreement 20.00) to 15; Save. → Reopen from the list; "Refresh from agreement". → "Delete saved rates" → confirm.
  - Expected: Agreement select disabled until a territory ("Loading agreements…", "No agreements published for this territory"); GET /api/v2/deal-memo/agreements?territory=. Save disabled until dirty with an agreement; PATCH sends allowances 25 and 20 (15 floored to the agreement). Edit page "Edit Union Pay Breakdown" loads GET /ad-rate-config/union/:id; refresh toast "Refreshed from agreement — review and Save". Delete confirm "Delete union pay breakdown?" → DELETE /ad-rate-config/union/:id, toast "Union agreement deleted".

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Artiste queries

- **Production workspace** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Artiste raises a query (Supporting Artistes tool). → AD opens Artiste Queries and clicks it.
  - Expected: GET /api/v2/artiste-queries; rail "Artiste Queries" with "All"/"Active"/"Resolved" and search; no "New" button. Thread GET /artiste-queries/:id. "Show summary" pane "Day breakdown" from GET /api/v2/supporting-artist-days/:id: status ("Awaiting signature" for submitted), Call/Wrap as stored (UTC), pay lines, "Logged total"; no voucher link; code VCH-<ref>-<yyyymmdd>.

- **Reply** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Type and press Enter. → Shift+Enter. → Route reply → 500 and send.
  - Expected: POST /artiste-queries/:id/reply {text}, optimistic entry; Shift+Enter adds a line; failure marks the entry "Not sent" with "Retry" and discard. "You" labels only the viewer's own messages; other production users are not "You".

- **Resolve** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: "Mark resolved". → On another query route resolve → 500.
  - Expected: POST /artiste-queries/:id/resolve; composer replaced by "This query is resolved. Raise a query on the same day to reopen it." for both sides. Failure: no toast and the query stays active (probe).

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Wrap and AD reports

- **Merged Wrap Report list** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open Wrap Report with Wrap and AD reports on several days. → Filter Report "AD Report", Status "Drafts"; search "SD 3".
  - Expected: GET /api/v2/ad-report/wrap-reports and /ad-reports?include_untyped=1; newest day first, Wrap before AD on the same day; filters live in the URL query (kind=, status=); empty "No reports match." / "No reports yet."

- **Save menu and unsaved guard** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: On a never-saved draft click "Save as". → On a saved draft "Save" → "Save — Update this draft". → "Save as — Save a copy as a new draft" after changing the date. → Edit, then Back before autosave.
  - Expected: 2: PATCH notify:true, toast "Report saved". 3: POST with as_copy:true for the newly picked date, toast "Saved as a new draft". 4: "Unsaved changes" modal with "Save & leave" and discard; Back goes to the list, or to Shoot Schedules when opened from there.

- **One report per day and kind** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Change the report date to a day that already has this kind. → Save.
  - Expected: Date panel "A report already exists for this day … Open the existing report"; Save toasts "<day> already has a <report> — nothing was saved. Your draft is still open; change the report date to save it."; draft stays dirty.

- **Report times write back to the day** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: In a Wrap report fill Crew Call 07:30, Wrap 19:00 and a Set Call row; save. → GET /api/v2/ad-shoot-days/day-details?shoot_date=….
  - Expected: PATCH carries day_patch:{call_time:<07:30Z>, wrap_time:<19:00Z>, …} with empty fields left out; Set Call is not bound; Schedule row reads "Call 07:30 · Wrap 19:00"; day-details returns them (Timecard reads this).

- **Delete a draft** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Trash on a draft → "Delete this report?" → "Delete report". → Repeat with DELETE routed to 500.
  - Expected: Row disappears at once; DELETE …/:id. On failure the row comes back and the server message shows.

- **Templates page** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open /templates. → "Duplicate" a system template; "Open editor". → "New Template". → Delete the custom copy.
  - Expected: System cards (origin "Provided by Zillit", "Zillit draft", "Sample format") offer Duplicate and Open editor, no Delete; custom cards add Delete → "Delete this template?"; POST /api/v2/ad-report/templates/:id/duplicate; DELETE /ad-report/templates/:id.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › SAG-Aftra Exhibit G

- **Create and locked layout** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: SAG-Aftra Exhibit G → "Create SAG-Aftra Exhibit G". → Try to add, rename or resize a column. → "Add Row"; open a Cast cell.
  - Expected: Report for today on /api/v2/ad-report/exhibit-g-reports; list chips "All"/"Drafts"/"Published". No "Add Column", column names are text, no resize handles; Add Row and Delete row work. Cast offers only Cast & Talent project users and hides users already cast on another row.

- **Reset signatures** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: "Reset signatures" → confirm.
  - Expected: Confirm "Reset signatures?" ("This voids all {n} collected signatures…"); POST …/reset-signatures; rows return to "Not signed yet — sign here".

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Extras portal (/extras-portal/:token)

- **Gate errors** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Route request-otp → portal_otp_email_mismatch, portal_otp_no_email, invalid_token, token_expired, network error. → Route verify-otp → portal_otp_invalid, portal_otp_expired, portal_otp_used.
  - Expected: "No match.", "No email on file." (Send withdrawn), "Link unavailable.", "Link expired." (terminal), "Couldn't send the code."; "That code isn't right.", "Code expired.", "Code already used."

- **Profile validation** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Clear first name, DOB, e-mail, NI number, bank name, holder, account number and IBAN, category; "Submit details". → Enter IBAN only plus the rest.
  - Expected: Toast "Please fix the highlighted fields."; "First name is required.", "Last name is required.", "Date of birth is required.", "Email is required.", "Required.", "Enter an account number or an IBAN". IBAN without account number passes.

- **Approval edge cases (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Route approve → 500 and click. → Open the link for a day that is pending or blocked. → Route viewDay → token_expired, then any other error.
  - Expected: 1: expected an error message; code shows none. 2: expected no approve action; code shows "Approve this Day" for any status except signed/approved. 3: "Link expired" / "Invalid link" with "This approval link has expired or has already been used. …", "Try the link again" (reload) and "Contact the AD team".

- **Exhibit G sign-only link** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Open a per-slot signing token and verify. → "Sign (1 of N)" → draw → "Submit signature". → Route requests → {requests:[]}.
  - Expected: view 404s, then GET /exhibit-g-sign/<token>/requests; header "Production time report"; modal "Sign your row" / "Sign below"; POST /exhibit-g-sign/<token>/sign {report_id, cell_id, row_id, signature_image:<data URL>}; row "Signed". Empty: "Nothing to sign" / "This link is no longer active. …". Error: "Couldn’t save your signature. Please try again." or server text.

- **Session lapse and phone width** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Half-fill the form; expire the session (route calls → portal_otp_session_expired). → Re-verify. → Set the viewport to 375×812.
  - Expected: Verify card overlays the page; the form keeps its typed values after re-verifying. At 375 px no horizontal scroll; detail grid collapses to one column; the six code boxes fit.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Realtime and badges

- **Another AD's marks appear live** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: AD A and AD B open Today's List. → B marks present, marks out and adds an extra.
  - Expected: A's list updates without reload (socket ad_dashboard:shoot_schedule:* / today_list:* → one list GET).

- **Tab-level badges clear on entry** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Produce Today's List, Add Extras, report and Shoot Schedule notifications for user A. → A opens each tab (by sidebar and by URL).
  - Expected: Chips show counts ("99+" cap) and section totals on "Reports"/"Artistes". Entry emits notification:level:read once with unit today_list_label + level_3:'today', add_extras_label + level_3:'tomorrow', ad_reports_label + level_2:'report', shoot_schedule_label with no marker; no second emit on sub-routes.

- **Per-item badges** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Artiste updates profile; artiste raises a query. → AD opens Register row ("View") and the query.
  - Expected: Register row and query row show their own badge; opening sends level_3:<artiste id> / <query id> only for that item; sidebar chip counts down. Query list refreshes live even from the same device.

- **Concurrent Rates & Config edits** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: A edits a rate (unsaved). → B saves. → A saves.
  - Expected: A's page doesn't reload while dirty; A's save wins (last write); with no unsaved edits A reloads on ad_dashboard:ad_rate_config:updated.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Errors, empty and loading states

- **List load failure is silent** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Route GET /api/v2/supporting-artist-days → 500. → Open Today's List.
  - Expected: No crash; no error toast; the empty state "No extras added for this shoot day yet." shows (misleading; see defects). A later successful refetch keeps the last good data.

- **Per-tab loading and no flash** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Throttle the network; enter Today's List, Submit Day and Add Extras on a submitted day. → Return to a tab.
  - Expected: Skeletons on first load; Add/Submit controls don't appear and then vanish while the day header loads; re-entering refetches quietly (cards stay). Nothing is fetched at dashboard entry beyond the landing tab.

### 11 AD Dashboard and Supporting Artistes › AD Dashboard › Theme and windowed view

- **Own theme key** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: Click the sidebar theme toggle (aria "Toggle colour theme", label "Dark mode"/"Light mode", "Click to switch"). → Reload. → Set the Account Hub theme to dark elsewhere.
  - Expected: localStorage ad-dash-theme = dark; .ad-dash-root gets .ad-dark; persists after reload; default light; the closest #account-hub-root never carries .dark, whatever accounthub-theme holds.

- **Dark theme on every surface** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: In dark, visit each tab, open the artiste modal, voucher modal, Add Extra modal, signature picker, report editor and Artiste Queries.
  - Expected: Text stays readable, no white panels or light-on-light; Queries workspace renders dark (isDark). Repeat in light.

- **Windowed view** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/ad-dashboard
  - Steps: On an MDI project open the tile. → Navigate tabs, open an artiste, go back. → Open an Account Hub window in dark theme alongside.
  - Expected: Opens a tab window titled "AD Dashboard"; browser URL stays /workspace; in-window navigation and Back work; the other window keeps its dark theme.

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Access & navigation

- **Page routes and titles** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Click each sidebar item. → Open /film-tools/supporting-artists/nope. → Open /film-tools/supporting-artists/history.
  - Expected: Top bar: "Dashboard"/"Welcome back", "My Vouchers"/"All vouchers across productions", "Productions"/"All productions", "Pay History"/"Full pay history", "Profile"/"Your artiste profile" (Queries hides the top bar). /nope → /dashboard. Pay History lives at /pay-history; /history is not a portal page (it renders the generic tool history).

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Rights and artiste record

- **Agency artiste and signing (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Register an internal artiste with engagement Agency; book, present, submit the day. → Open the voucher as that artiste.
  - Expected: AD Submit Day says agency artistes are "covered by their manifest countersign", yet the portal shows the Sign card for any submitted voucher. Record what shows and whether the sign call succeeds.

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Dashboard

- **KPIs** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Open Dashboard.
  - Expected: GET /api/v2/sa-portal/summary, /vouchers, /productions, /me. Tiles "Vouchers this year" ("{signed} signed · {paid} paid · {pending} pending"), "YTD gross earned" ("Current tax year · pre-tax"), "Holiday pay accrued" ("12.07% · paid at wrap of contract"), "Productions" ("This production"). Amounts use the artiste's currency, else the project default, two decimals.

- **Production, recent vouchers, status, holiday** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Read the lower cards. → Click "Full history".
  - Expected: "Current Production" (Days worked, Gross earned, Holiday accrued, "Next booking — {date}", "Call {time}"); "Recent Vouchers" = first three in server order; "Account Status" % with "All sections complete"/"Some sections outstanding"; "Holiday Pay Tracker". "Full history" → Pay History.

- **Earnings toggle (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: In "Earnings" switch "6 mo" ↔ "Tax year".
  - Expected: Expected: the chart range changes. Code keeps plotting summary.earnings unchanged.

- **No summary** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Route GET /sa-portal/summary → {data:null}, then → 500.
  - Expected: Both show "No dashboard yet — Your artiste record for this production isn't set up." (a failed fetch is not distinguished).

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › My Vouchers

- **List and badge read** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Open My Vouchers.
  - Expected: GET /api/v2/sa-portal/vouchers; on mount emits notification:level:read with unit my_vouchers_label and level_2:'voucher'; rows show date, code, "Call {call} · Wrap {wrap} · {duration}", gross, status pill.

- **Filters** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Click "All", "To sign", "Signed", "Paid".
  - Expected: "To sign" = day_status: submitted; "Signed" = status: signed; "Paid" = status: paid; empty filter "No vouchers match this filter".

- **Status pills** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Route vouchers with day_status submitted, blocked, signed, approved and status pending, paid.
  - Expected: Pills "Sign now", "Blocked", "Signed", "Paid" (approved), "Pending", "Paid"; a voucher with status: paid and day_status: signed shows "Signed" (first match wins).

- **Export** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: "Export all". → On a voucher detail "Export PDF". → Route the export → 500.
  - Expected: POST /api/v2/sa-portal/vouchers/export {voucher_id:null} → vouchers-YYYYMMDD.pdf; detail {voucher_id} → voucher-<code>.pdf; button "Exporting…". Failure toast "Could not export — please try again" (drawn with the success tick; see defects).

- **Empty list** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Artiste with no vouchers opens My Vouchers.
  - Expected: "Vouchers appear here once you've worked a shoot day on this production."

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Voucher detail and signing

- **Unknown voucher code** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Open /film-tools/supporting-artists/voucher/NOPE.
  - Expected: "Voucher not found" — "This voucher may have been removed or doesn't belong to you."

- **Sign failure** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Route the sign POST → 400 {message:'some_key'}. → Sign.
  - Expected: Toast shows the server text as sent (raw key) or "Could not sign — please try again."; button usable again; voucher still unsigned.

- **Raise a query from the voucher** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Click "Raise a query about this voucher". → Go back; click "View all queries".
  - Expected: Opens /queries?voucher=<dayId> with the composer open and that day preselected; "View all queries" opens Queries. Signing remains possible afterwards.

- **Open detail doesn't update live (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Artiste opens a Pending voucher. → AD submits that day.
  - Expected: Expected: the Sign card appears. Code refreshes the list but not the open detail, so it appears only after reopening.

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Queries

- **Queries page** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Open Queries.
  - Expected: Top bar hidden; rail "Queries" with "New", "All"/"Active"/"Resolved", "Search queries…"; empty "No queries yet" + "Raise a query about a shoot day and track the reply here."; nothing selected: "Select a query" / "Pick a query from the list to open its record, reply, and see the day breakdown." No tab-level badge read on entry.

- **Day that already has a thread** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Pick a day with an existing (even resolved) thread. → Submit.
  - Expected: Notice "This day already has a query thread — your message will be added to it."; button "Add to thread"; toast "Added to that day’s existing query thread"; a resolved thread is reopened by the server.

- **Reply** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Type a reply, press Enter. → Route reply → 500 and send.
  - Expected: POST /api/v2/sa-portal/queries/:id/reply {text}, optimistic; failure shows "Not sent" with "Retry" and discard. Own messages read "You", production messages "Production".

- **Mark resolved** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: On an active query click "Mark resolved".
  - Expected: POST /api/v2/sa-portal/queries/:id/resolve; toast "Query marked as resolved"; reply box replaced by "This query is resolved. Raise a query on the same day to reopen it." (also on the AD side).

- **Day breakdown pane** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Open a query; toggle "Hide summary"/"Show summary". → Click "Day voucher".
  - Expected: GET /api/v2/sa-portal/vouchers/:id; "Day breakdown" with "Shoot day", status (submitted → "Awaiting signature"), "Call", "Wrap", pay lines, "Logged total"; "Linked documents" → "Day voucher" opens the voucher detail.

- **Per-query badges** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: AD replies to two queries. → Artiste opens one.
  - Expected: Each row shows its own unread badge ("99+" cap); opening sends notification:level:read with level_3:<query id> for that query only; the sidebar Queries chip drops by that amount.

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Profile

- **Read view** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Open Profile. → Open an uploaded document.
  - Expected: GET /me; read-only sections (personal, tax, bank masked, ID, documents, booking) and "Edit details"; emits notification:level:read unit profile_label, level_2:'artiste'. PDF documents open in the shared PDF viewer, images in an overlay, both in the portal theme.

- **IBAN alone is enough** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Fill all required fields with an IBAN and no account number. → Save.
  - Expected: Saves without a bank error.

- **Bank status after save (probe)** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Save complete bank details. → Check Dashboard → Account Status.
  - Expected: Expected: Bank details complete. The portal save sends no bank_id, and the ring counts bank only with a linked bank_id, so it may stay "Pending".

- **Add a new agency from the profile** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: In edit, Engagement "Agency" → "Add a new agency" → fill → save.
  - Expected: Agency picker lists GET /api/v2/ad-rate-config/vendors; the overlay POSTs /api/v2/ad-rate-config/vendors. Record whether the server accepts it from an artiste (see Open questions).

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Pay History and Productions

- **Pay History** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Open Pay History.
  - Expected: GET /api/v2/sa-portal/pay and /summary; tiles "Gross (this year)", "Gross (total)", "Holiday (total)" ("{paid} paid · {accrued} accrued"); "Pay runs" rows with week start (UTC) or "Unscheduled", "{n} days · payroll week", gross, "Pending" for status: pending, else "Paid"; "Holiday Pay" panel "Accrued at 12.07% of gross · paid at contract end"; empty "No pay runs yet".

- **Productions** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Open Productions. → Click "View vouchers".
  - Expected: GET /api/v2/sa-portal/productions; one card per row with shoot days, gross, holiday accrued, pending voucher count; footer totals; "View vouchers" → My Vouchers. Every card is titled with the current project's name and "Active" (probe). Empty: "No productions yet".

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Voucher portal (/voucher-portal/:token)

- **Gate errors** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Route request-otp → sa_portal_otp_email_mismatch, sa_portal_otp_no_email, artiste_not_found_for_user, invalid_token, token_expired.
  - Expected: "No match.", "No email on file." (terminal), "Record unavailable." ("We can't find your artiste record for this voucher. …"), "Link unavailable." ("This voucher link is not valid. …"), "Link expired."

- **Voucher view** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: After verifying, read the page.
  - Expected: "Supporting Artiste Voucher", "<date> · <project name>", the same voucher body as in-app; light theme regardless of sa-portal-theme; footer "Powered by ZILLIT · {year}". Amounts show without a currency symbol (probe).

- **Sign with a typed signature** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: In "Sign your voucher" choose type, enter a name, use it, sign.
  - Expected: sign_status:'typed', typed_name set; the in-app voucher's signed proof later reads "Typed signature".

- **Not signable** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Open a link for a signed voucher, then for a not-yet-submitted voucher.
  - Expected: Signed: signed panel, no sign card. Not submitted: voucher body only, no sign card.

- **Load and sign errors** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Route GET → 404 with no message, then with {message:'some_key'}. → Expire the session mid-page (sa_portal_otp_session_expired) and re-verify. → Route sign → 400 {message:'x'}.
  - Expected: 1: "Voucher unavailable" + "This voucher link is no longer available." / the raw key. 2: verify card overlays; after re-verifying the voucher reloads (no stuck error). 3: the raw server text shows under the button.

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Realtime and badges

- **Sidebar chips** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Produce voucher, query and profile notifications. → Open My Vouchers.
  - Expected: Chips only on My Vouchers, Queries and Profile ("99+" cap); My Vouchers chip clears on opening the tab; Queries chip clears per query.

- **AD reply appears in the open thread** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Artiste has a query open. → AD replies (also from another tab on the same device).
  - Expected: The reply appears in the thread and the rail without reload; same-device events are not deduplicated.

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Errors, empty and loading states

- **Per-page loading and silent errors** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Throttle; open each page. → Route GET /sa-portal/vouchers → 500.
  - Expected: First load shows skeletons; later visits refetch quietly. A failed fetch shows the empty state with no error toast (see defects).

### 11 AD Dashboard and Supporting Artistes › Supporting Artistes › Theme and windowed view

- **Own theme key** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Click the sidebar theme toggle ("Dark mode"/"Light mode"). → Reload. → Open the new-query composer and a document preview in dark.
  - Expected: localStorage sa-portal-theme = dark; .sa-portal-root gets theme-dark; closest #account-hub-root has no .dark; persists after reload; body-level overlays (composer, document viewer) render dark too.

- **Public portal stays light** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: Set sa-portal-theme to dark. → Open /voucher-portal/<token>.
  - Expected: Gate and voucher page render light.

- **Windowed view** — planned (e2e/account-hub/11-ad-dashboard-and-supporting-artists.md)
  - Where: /film-tools/supporting-artists
  - Steps: On an MDI project open the tile. → Move between Dashboard, a voucher, Queries and back.
  - Expected: Tab window "Supporting Artists"; URL stays /workspace; nested pages and Back work inside the window.

## Sheet bugs (0)
