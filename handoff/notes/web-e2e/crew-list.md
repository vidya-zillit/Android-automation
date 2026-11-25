# Web E2E — Crew List

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: none — 

## Web test cases (0)

## Sheet bugs (2)

- **CREW-01** A member's contact sheet is missing the Zillit e-mail — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open Crew List. 2. Tap a member who has a Zillit e-mail.
  - Expected: CONTACT DETAILS shows a Zillit E-mail row.
  - Actual: No Zillit E-mail row; the section was hidden when only the Zillit e-mail was set.

- **CREW-02** Submitting the empty external crew form gives no clear error — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open the add-external-user form from Crew List. 2. Tap Submit with every field empty.
  - Expected: Alert "Please fill all the fields." and nothing is created.
  - Actual: The validation alert did not appear reliably (it went through a shared view-model alert).
