# Web E2E — Settings

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: moderate — Admin, Profile and User Management screens

## Web test cases (36)

### Admin Settings hub (Settings ▸ Admin Settings)

- **the hub renders the core tiles @smoke** — passed (adminsettings.spec.js:94)
  - Where: Settings → Admin Settings tab · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Check that these tiles are shown: Edit Project Name, Create New Dept, Create New Designation, Customization of Tools, Pre-Approved Users, Watermark Logo of Company → Check that these tiles are shown too: User Management, Viewing & Posting Rights Grid, Approve User Profile, Approve New Request, Set/View SOS Receivers, Listing Order for Crew List (or Staff List), Manage Tool Groups, Delete Project → Check that the Company Details tile is shown
  - Expected: The Admin Settings hub shows all of its core tiles, including Company Details.

- **User Management tile lands on the member-management screen @smoke** — passed (adminsettings.spec.js:122)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the User Management tile → Check that a search box is shown → Check that the member table shows at least one member row
  - Expected: The User Management tile opens the member-management screen with its search box and a populated member table.

- **Viewing & Posting Rights Grid tile navigates away from the hub @smoke** — passed (adminsettings.spec.js:135)
  - Where: Settings → Admin Settings tab → Viewing & Posting Rights Grid · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Viewing & Posting Rights Grid tile → Check that the Admin Settings tiles are no longer shown (the app has moved to another screen)
  - Expected: The Viewing & Posting Rights Grid tile takes you away from the hub. (The test does not check what the new screen shows.)

- **Approve New Request tile navigates away from the hub @smoke** — passed (adminsettings.spec.js:143)
  - Where: Settings → Admin Settings tab → Approve New Request · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Approve New Request tile → Check that the Admin Settings tiles are no longer shown (the app has moved to another screen)
  - Expected: The Approve New Request tile takes you away from the hub. (The test does not check what the new screen shows.)

- **Create New Department — the create form + department list @smoke** — passed (adminsettings.spec.js:152)
  - Where: Settings → Admin Settings tab → Create New Dept · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Create New Dept tile → Check that the Create New Department dialog shows its Save Department button → Check that it shows a text field for the department name → Check that it lists at least one existing department → Click Cancel without saving
  - Expected: The Create New Department dialog shows the name field, Save Department and the list of existing departments. Nothing is created.

- **Create New Designation — the department select + add form @smoke** — passed (adminsettings.spec.js:168)
  - Where: Settings → Admin Settings tab → Create New Designation · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Create New Designation tile → Check that the dialog shows its Add button → Check that it shows a department dropdown → Check that it shows a text field for the designation → Press Escape to close the dialog without adding anything
  - Expected: The Create New Designation dialog shows the department dropdown, the designation field and the Add button. Nothing is created.

- **Edit Project Name — modal shows the prefilled name + Submit/Cancel @smoke** — passed (adminsettings.spec.js:185)
  - Where: Settings → Admin Settings tab → Edit Project Name · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Edit Project Name tile → Check that the dialog shows Submit and Cancel → Check that the name field is already filled with the current project name → Click Cancel
  - Expected: The Edit Project Name dialog opens with the current name filled in and Submit/Cancel buttons. The project is not renamed.

- **Approve User Profile — opens the select-user modal @smoke** — passed (adminsettings.spec.js:201)
  - Where: Settings → Admin Settings tab → Approve User Profile · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Approve User Profile tile → Check that a dialog opens → Press Escape to close it
  - Expected: The Approve User Profile tile opens its dialog. (The test only checks that a dialog appears, not its content.)

- **Create/Update Home Unit — opens the unit list modal @smoke** — passed (adminsettings.spec.js:211)
  - Where: Settings → Admin Settings tab → Create/Update on Home Unit · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Create/Update on Home Unit tile → Check that the unit list dialog opens → Check that it has an input field (its search box) → Press Escape to close it
  - Expected: The Create/Update on Home Unit tile opens the unit list dialog with its search box.

- **Tools Customization — tool list with Save/Cancel; cancel @smoke** — passed (adminsettings.spec.js:222)
  - Where: Settings → Admin Settings tab → Customization of Tools · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Customization of Tools tile → Check that the dialog shows Save and Cancel → Check that at least one tool checkbox is shown → Click Cancel
  - Expected: The Customization of Tools dialog lists tools with checkboxes and offers Save and Cancel. Nothing is saved.

- **Manage Tool Groups — opens with the two tabs @smoke** — passed (adminsettings.spec.js:238)
  - Where: Settings → Admin Settings tab → Manage Tool Groups · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Manage Tool Groups tile → Check that the dialog opens with exactly two tabs (Tool Groups and Assign Tools) → Press Escape to close it
  - Expected: The Manage Tool Groups dialog opens with its two tabs.

- **Change Priority — reorder list with Save disabled; cancel @smoke** — passed (adminsettings.spec.js:250)
  - Where: Settings → Admin Settings tab → Listing Order for Crew List · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Listing Order for Crew List (or Staff List) tile → Check that the dialog shows Cancel → Check that Save is disabled while the order is unchanged → Click Cancel
  - Expected: The listing-order dialog opens with Save disabled until the order is changed. Nothing is saved.

- **Watermark Logo — upload control + Cancel; cancel @smoke** — passed (adminsettings.spec.js:262)
  - Where: Settings → Admin Settings tab → Watermark Logo of Company · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Watermark Logo of Company tile → Check that the dialog shows Cancel → Check that it shows the Upload button → Click Cancel
  - Expected: The Watermark Logo dialog opens with its Upload and Cancel buttons. No logo is uploaded.

- **Pre-Approved Users — create form with Submit; cancel (no submit) @smoke** — passed (adminsettings.spec.js:272)
  - Where: Settings → Admin Settings tab → Pre-Approved Users · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Pre-Approved Users tile → Check that the dialog with the Submit button opens → Check that its form shows input fields (first name and so on) → Press Escape to close it without submitting
  - Expected: The Pre-Approved Users dialog opens with its form and Submit button. Nothing is submitted, so no user is created.

- **Company Details — opens the drawer with the company form @smoke** — passed (adminsettings.spec.js:285)
  - Where: Settings → Admin Settings tab → Company Details · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Company Details tile → Check that a side panel opens → Check that it shows the company form (a text field such as the company name) → Check that it shows a Save button → Press Escape to close it
  - Expected: Company Details opens a side panel with the company form and a Save button. Nothing is saved.

- **Set/View SOS — opens the SOS surface @smoke** — passed (adminsettings.spec.js:300)
  - Where: Settings → Admin Settings tab → Set/View SOS Receivers · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Set/View SOS Receivers tile → Check that a dialog or side panel opens → Press Escape to close it
  - Expected: The Set/View SOS Receivers tile opens its SOS screen. (The test only checks that a dialog or panel appears.)

- **Remote Shooting Unit — opens the unit list modal (if the tile shows) @smoke** — passed (adminsettings.spec.js:310)
  - Where: Settings → Admin Settings tab → Create Remote shooting unit · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Look for the Create Remote shooting unit tile → Click it → Check that the unit list dialog opens with its close button → Close the dialog
  - Expected: The Create Remote shooting unit tile opens the remote unit list dialog.

- **Delete Project — shows timeframe options + disabled Confirm; close (never deletes) @smoke** — passed (adminsettings.spec.js:327)
  - Where: Settings → Admin Settings tab → Delete Project · account: iPhone XRR
  - Steps: Click Settings in the sidebar, then the Admin Settings tab → Click the Delete Project tile → Check that the dialog offers “Delete in 12 hours”, “Delete in 24 hours” and “Delete in 48 hours” → Do not pick any option → Close the dialog with its X (or Escape)
  - Expected: The Delete Project dialog shows the three timeframe options. The project is never deleted. (Despite the title, the test does not check that Confirm is disabled.)

### Settings ▸ Profile Setting

- **Invite User — "share via internal email" opens the in-app compose modal @smoke** — passed (profilesettings.spec.js:345)
  - Where: Settings → Profile Setting tab → Invite User → Share · account: iPhone XRR
  - Steps: Click Settings in the sidebar (the Profile Setting tab opens by default) → Click Invite User → Click Share → Click “Share via Internal Email” → Check that the in-app email compose window (a dialog or side panel) opens
  - Expected: “Share via Internal Email” opens the in-app email composer. Nothing is sent.

### User Management

- **page loads with search box and a populated member table @smoke** — passed (usermanagement.spec.js:170)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Check that the search box is shown → Check that the member table lists at least one member
  - Expected: User Management opens with its search box and a member table that has at least one row.

- **each member row exposes Active/Admin switches and row actions @smoke** — passed (usermanagement.spec.js:180)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Look at the first member row → Check that it has exactly two switches (Active and Admin) → Check that it shows the Edit User Details button → Check that it shows the Posting Rights button
  - Expected: Each member row offers the Active and Admin switches plus the Edit User Details and Posting Rights buttons.

- **search filters the member list to a match @smoke** — passed (usermanagement.spec.js:195)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Wait until the member list stops changing and count the rows → Take the first 6 letters of the first member’s name → Type them in the search box → Check that the first row left contains the typed letters (case ignored) → Check that the list has no more rows than before
  - Expected: Searching filters the member table: the top row matches the search text and the list does not grow.

- **edit icon opens the Change Profile modal and closes clean @smoke** — passed (usermanagement.spec.js:327)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first active member → Click their Edit User Details button → Check that the Change Profile dialog opens with its dropdowns (Department, Designation and so on) → Check that it shows a Submit button → Close the dialog with its X button → Check that no dialog is left open
  - Expected: Edit User Details opens the Change Profile dialog with its dropdowns and Submit, and closing it leaves nothing open. Nothing is saved.

- **posting-rights modal opens with the section switch and rights table @smoke** — passed (usermanagement.spec.js:411)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first active member → Click their Posting Rights button → Check that the dialog shows the Home/Tools section dropdown in its title → Wait for the rights table to load → Check that the first unit row shows its switches → Press Escape to close the dialog
  - Expected: The Posting Rights dialog opens with the Home/Tools section switch and a rights table of units with switches.

- **switching the section Home ⇄ Tools swaps the unit list @smoke** — passed (usermanagement.spec.js:425)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first active member and click their Posting Rights button → Note the section shown in the dialog’s dropdown (Home or Tools) → Open the dropdown and check that it offers exactly two sections → Choose the other section → Check that the dropdown now shows that section and the rights table is shown → Press Escape to close the dialog
  - Expected: Switching the Posting Rights dialog between Home and Tools changes the section shown and redraws the rights table.

- **non-updatable rights switches are disabled and inert @smoke** — skipped (usermanagement.spec.js:533)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first active member and click their Posting Rights button → Find a switch in the rights table that is greyed out (cannot be changed) → Note whether it is on or off → Click it → Check that it has not changed → Press Escape to close the dialog
  - Expected: A greyed-out rights switch ignores clicks and keeps its state.

- **deactivate then reactivate a member; deactivated actions are gated** — passed (usermanagement.spec.js:232)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first member whose Active switch is on → Turn their Active switch off and wait for it to settle → Click that member’s Posting Rights button → Check that a warning message appears and no dialog opens → Turn their Active switch back on and wait for it to settle
  - Expected: A member can be deactivated and reactivated; while deactivated, their Posting Rights button only shows a warning instead of opening.

- **promote a member to admin (with confirmation) then demote** — passed (usermanagement.spec.js:262)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first active member and note whether they are an admin → If they are an admin, turn their Admin switch off first → Turn their Admin switch on → Check that the “Confirm Admin Rights” dialog appears and click Yes → Check that the Admin switch is now on → Turn the Admin switch off again (no dialog) and check that it stays off
  - Expected: Making a member an admin asks for confirmation and grants admin on Yes; turning the switch off removes admin without a dialog.

- **cancelling the promote confirmation leaves the member unchanged** — passed (usermanagement.spec.js:302)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first member who is active but not an admin → Turn their Admin switch on → Check that the “Confirm Admin Rights” dialog appears → Click Cancel → Check that the dialog closes and the Admin switch is still off
  - Expected: Cancelling the admin confirmation leaves the member as a non-admin.

- **grant then revoke a unit View right (clean restore)** — passed (usermanagement.spec.js:456)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first member who is active but not an admin and click their Posting Rights button → Find the first unit whose View switch is off and can be changed (look in Home, then in Tools) → Turn that View switch on and wait for it to settle → Turn it off again and wait for it to settle → Press Escape to close the dialog
  - Expected: A unit’s View right can be granted and then removed for the member.

- **grant then revoke a unit Post right (auto-enables View)** — passed (usermanagement.spec.js:479)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first member who is active but not an admin and click their Posting Rights button → Find the first unit whose Post switch is off and can be changed (look in Home, then in Tools) → Note whether that unit’s View switch is on → Turn the Post switch on and wait for it to settle → Check that the View switch turned on as well → Turn the Post switch off again → If View was off before, turn View off again → Press Escape to close the dialog
  - Expected: Granting a unit’s Post right also turns on its View right, and the Post right can then be removed.

- **grant then revoke a unit Download right** — passed (usermanagement.spec.js:512)
  - Where: Settings → Admin Settings tab → User Management · account: iPhone XRR
  - Steps: Open Home, then Settings → Admin Settings tab → User Management → Pick the first member who is active but not an admin and click their Posting Rights button → Find the first unit whose Download switch is off and can be changed (look in Home, then in Tools) → Turn that Download switch on and wait for it to settle → Turn it off again and wait for it to settle → Press Escape to close the dialog
  - Expected: A unit’s Download right can be granted and then removed for the member.

### Settings ▸ Profile Setting

- **Edit Profile — update first/last name, verify via the profile API, then restore** — notrun (profilesettings.spec.js:141)
  - Where: Settings → Profile Setting tab → Edit Profile · account: iPhone XRR
  - Steps: Click Settings in the sidebar (the Profile Setting tab opens by default) → Click Edit Profile → Note the current first and last name → Type a short unique first name such as “E2eFn12345” and last name such as “E2eLn12345” → Click Submit → Check that the save succeeds and the saved profile carries the new first and last name → Check that the dialog closes
  - Expected: Edit Profile saves the new first and last name for the account (the test confirms it from the saved profile the server sends back, not from the screen).

- **Edit Preference — update phone + add other info, verify via the profile API, then restore** — notrun (profilesettings.spec.js:202)
  - Where: Settings → Profile Setting tab → Edit Preference · account: iPhone XRR
  - Steps: Click Settings in the sidebar (the Profile Setting tab opens by default) → Click Edit Preference and wait for the form to fill in (the Change Unit field shows a unit) → Note the current phone number → Type “5551234567” as the phone number; if no country code is set, pick the first one in the list → Click Add More Information and type “E2E Field” and “E2E Value” in the new row → Check that the Gender and Email fields are shown → Click Submit → Check that the save succeeds and carries the new phone number and the “E2E Field” row → Check that the side panel closes
  - Expected: Edit Preference saves the new phone number and the extra information row (the test confirms it from the save request, not from the screen).

- **Invite User — copy the invite message and post it to the Home Bulletin** — notrun (profilesettings.spec.js:314)
  - Where: Settings → Profile Setting tab → Invite User, then Home → Bulletin tab · account: iPhone XRR
  - Steps: Click Settings in the sidebar (the Profile Setting tab opens by default) → Click Invite User → Click Share, then Copy Link → Check that an invite message was copied to the clipboard → Open Home and make the Bulletin tab the active tab → Paste the invite message into “Type your message here”, prefixed with a marker such as “[e2e-invite <time>]” → Press Enter → Check that the message with the marker appears in the Bulletin feed
  - Expected: Copy Link puts a non-empty invite message on the clipboard, and that message can be posted to the Home Bulletin.

- **Web Preferences — changing the default Home unit opens that unit on entry** — notrun (profilesettings.spec.js:360)
  - Where: Settings → Profile Setting tab → Web Preferences · account: iPhone XRR
  - Steps: Click Settings in the sidebar (the Profile Setting tab opens by default) → Click Web Preferences → Open the Default Home Unit dropdown and choose Calendar (it saves straight away) → Close the dialog → Open the app in a new browser window with the same account and enter the project from the project list → Check that Home opens with the Calendar tab selected
  - Expected: After choosing Calendar as the Default Home Unit, entering the project opens Home on the Calendar tab.

## Sheet bugs (3)

- **SET-01** The success alert after saving the profile closes by itself and the screen stays on the form — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open Home once, then Settings > Edit My Profile. 2. Change last name or designation and Submit (repeat a few times).
  - Expected: The "Profile updated successfully." alert stays until Ok, and Ok returns to Settings.
  - Actual: On 3 of 7 Submits the alert closed after about 0.4 s by itself and the screen stayed on the form.

- **SET-02** Privacy Policy and Terms Of Use open blank — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open Settings. 2. Tap Privacy Policy, then Terms Of Use.
  - Expected: The page content shows.
  - Actual: Blank page.

- **SET-03** The notification setting opens the main iOS Settings screen instead of Zillit's page — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open Settings. 2. Tap the notifications setting.
  - Expected: iOS Settings opens on Zillit's notification page.
  - Actual: The main iOS Settings screen opened.
