# Web E2E — SOS

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (0)

## Sheet bugs (21)

- **SOS-01** After Cancel on the "mobile number is not updated" alert, the Member tab's Submit does nothing — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. As an admin whose profile has no mobile number, open Admin Settings → SOS Receivers. 2. Tap the Outsider segment: the alert "Your mobile number is not updated…" appears. Tap Cancel. 3. Back on Member, tap Submit with nothing picked, then pick a member and tap Submit.
  - Expected: Empty Submit marks the Member fields red; with a member picked, the receiver is added.
  - Actual: Submit does nothing in both cases (no red fields, no row, no alert) until the screen is reopened.

- **SOS-02** An admin can add themselves as their own SOS receiver — Low · Web: Not checked, Android: Not checked
  - Steps: 1. As an admin open Admin Settings → SOS Receivers. 2. On Member, pick yourself and Submit.
  - Expected: Decided by product: refused, or allowed on purpose.
  - Actual: The admin is saved as their own SOS receiver.

- **SOS-03** SOS sent from desktop never carries the sender's location, although the page promises it — High · Web: Not checked, Android: Not checked
  - Steps: 1. Open SOS. 2. Read 'Raise the alarm and your location goes to every receiver'. 3. Send SOS > Send SOS now ('...with your location'). 4. On a receiver, open the alert's location.
  - Expected: Alarm carries {lat, long}; receivers get a map link.
  - Actual: The app builds SosViewModel without a locationFix, so the default { null } is used and every desktop alarm posts an empty body; receivers get no location.

- **SOS-04** An SOS alarm that arrives while the feed is loading is not shown — High · Web: Not checked, Android: Not checked
  - Steps: 1. Open the SOS window (first page loading) or press Refresh. 2. While it loads, someone raises SOS on set.
  - Expected: The new alert appears.
  - Actual: refresh() returns early while loading, so the socket's refetch is dropped; the page in flight was answered before the alarm, so the alert stays hidden until a manual refresh/reopen.

- **SOS-05** Relationship shown as a raw label key in the dropdown and the receivers list — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Open SOS > Receivers > Outsider. 2. Open Relationship. 3. Look at a saved outsider's row.
  - Expected: Translated relationship names.
  - Actual: The department_name key is shown as-is (RelationField options and ContactRowMeta).

- **SOS-06** Reopening SOS shows a stale receivers list — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open SOS. 2. Close it; add or remove a receiver in Settings > SOS receivers (or on a phone). 3. Reopen SOS.
  - Expected: Receivers re-read; reach pill correct.
  - Actual: Contacts are only loaded when the list is empty, so the first visit's list (and 'Reaches N…') stays for the whole run.

- **SOS-07** Switching tab while editing overwrites the other kind of receiver — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Edit an outsider. 2. Switch to Member (banner still 'Editing …'). 3. Click Use beside a crew member.
  - Expected: Tab switch cancels the edit, or Use adds a new member.
  - Actual: PUT sos/contact/update/{outsiderId} with {user_id}: the outsider becomes a member. The reverse (member edit then Outsider submit) sends updateExternal on the member row.

- **SOS-08** Alarm refetch has no delay or retry for the backend lag the web works around — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Someone raises SOS. 2. The feed API has not yet indexed the row when the socket event lands.
  - Expected: The alert still shows.
  - Actual: Desktop refetches at once, once; if the row is not yet returned the alarm is missing until the next refresh.

- **SOS-09** Alerts deleted/cleared on another device stay on an open SOS window — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Open SOS on desktop. 2. Delete an alert or Clear all on the phone/web.
  - Expected: Desktop list updates.
  - Actual: SOS only listens to projectsos:alert:sent; notification:delete:sync is consumed only by the badge ledger, so the row stays until reopen/refresh.

- **SOS-10** SOS alerts are silenced by the general 'Activity' notification switch — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Settings > Notifications: turn Activity off. 2. Someone raises SOS while the app is in the background.
  - Expected: SOS still surfaces (it is an emergency; phones use a dedicated SOS push).
  - Actual: sos_label records go through ActivityAlerts and answer to NotifyActivity, so they are dropped; no distinct sound/urgency either.

- **SOS-11** After a failed first feed, another production shows the previous production's receivers — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open SOS on production A while the notification host is down (receivers load). 2. Switch to production B and open SOS.
  - Expected: B's receivers.
  - Actual: A's receivers stay listed and counted on the hero; edits/deletes would target A's rows. The reset only runs when the feed had loaded.

- **SOS-12** Settings > SOS receivers outsider form skips the SOS tool's validation and catalogues — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Settings > SOS receivers > Add outsider. 2. Type relationship 'Nurse', code '44x', phone '77-00-90-01'. 3. Add.
  - Expected: Relationship from sos/departments, code from the ISD list, phone 5–20 digits only (as web, which reuses the SOS component).
  - Actual: Free text for relationship and code; phone accepted on length 5–20 including dashes; the SOS tool then shows that free text.

- **SOS-13** Removing one receiver discards an edit in progress on another — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Edit outsider A and change the phone. 2. Remove receiver B and confirm.
  - Expected: A's edit and typed values stay.
  - Actual: Form is cleared and edit mode cancelled (DeleteContact applies cleared()).

- **SOS-14** Enter does not submit the outsider form — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Fill the outsider form. 2. Press Enter in Mobile number.
  - Expected: Form submits.
  - Actual: Nothing happens; fields have no onImeAction.

- **SOS-15** Alerts card is blank after a failed first load is dismissed — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open SOS while the notification host is unreachable. 2. Dismiss the error.
  - Expected: Card says alerts could not be loaded, with a retry.
  - Actual: Card body is empty (loaded=false so no 'All quiet', loading=false so no skeleton); only the header's icon-only refresh remains.

- **SOS-16** Alert from a crew member reads 'Unknown sender' when the payload lacks the name — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Receive an alert whose messageElements[0] is empty from a current crew member.
  - Expected: Card named from the crew list.
  - Actual: Card shows 'Unknown sender'; the crew list is only consulted for calling.

- **SOS-17** No 'Disconnected' tag for alerts from crew who left/were removed — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Receive an alert from someone later removed from the production.
  - Expected: Card tagged Disconnected, actions greyed.
  - Actual: No tag; call buttons enabled and only answer with a toast when pressed.

- **SOS-18** No way to the profile from the 'add a mobile first' notice — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Clear the mobile number on your profile. 2. Open SOS > Receivers > Outsider.
  - Expected: A button that opens profile preferences to add the number.
  - Actual: Only a notice; the user has to find Preferences themselves.

- **SOS-19** 'N more — search to narrow the list.' is hard-coded English — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Switch the app to French. 2. Open Member tab on a crew larger than 30.
  - Expected: Line translated.
  - Actual: Line stays English.

- **SOS-20** Esc does not close the SOS confirmation dialogs — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Click Send SOS (or a delete). 2. Press Esc.
  - Expected: Dialog closes.
  - Actual: Nothing happens.

- **SOS-21** Settings > SOS receivers crew picker offers existing receivers and the admin themself — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Settings > SOS receivers > Add crew.
  - Expected: Existing receivers and self excluded.
  - Actual: All active crew offered; a duplicate can be added.
