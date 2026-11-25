# Web E2E — Tasks

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: none — 

## Web test cases (0)

## Sheet bugs (4)

- **TASK-01** Task panel footer "Request access" closes the panel but never shows the Permission alert — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. As a user with the Tasks view right but no posting right, open Tasks. 2. Open a project task. 3. Tap Request access in the panel footer.
  - Expected: The panel closes and the alert "Permission" / "You do not have Posting Rights on this ‘Tasks’. Do you want to request one of the Admins to give you that permission?" appears with Cancel / Yes, as it does from the top-bar button.
  - Actual: The panel closes and no alert appears (2 of 2 tries). The top-bar Request access button does show the alert.

- **TASK-02** Edit-conflict footer buttons are cut off on iPhone 17 Pro — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open the same task on two devices. 2. Edit and save it on the second device. 3. Edit it on the first device and look at the amber footer.
  - Expected: Both buttons, "Load their version" and "Save mine anyway", are fully readable.
  - Actual: The labels are truncated: "Load their v…" and "Save mine an…".

- **TASK-03** With a Buddhist calendar, the date picker shows Buddhist years but task date labels stay Gregorian — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Set the device calendar to Buddhist (on the Simulator: launch with -AppleLocale en_US@calendar=buddhist). 2. Open Tasks and open a due-date picker.
  - Expected: Picker and labels use the same calendar (product decision).
  - Actual: The picker reads "October 2569 BE"; the board labels and chips stay Gregorian. Dates are saved correctly and nothing shows as overdue.

- **TASK-04** When the Tasks view right is removed, a "no access" panel flashes before the app returns to Tools — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open Tasks on device B. 2. On device A (admin), turn off B's Tasks view right.
  - Expected: B goes straight back to Tools.
  - Actual: A "no access" panel shows for a moment, then B returns to Tools with the tile hidden.
