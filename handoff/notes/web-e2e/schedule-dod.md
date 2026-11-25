# Web E2E — Schedule DOD

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (0)

## Sheet bugs (1)

- **DOD-01** An undated D.O.D. upload made right after a dated one is saved with the earlier date — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Upload a D.O.D. with a date. 2. In the same session, upload another without picking a date.
  - Expected: The second card reads "D.O.D. Date: Not Available".
  - Actual: The second upload kept the first one's date.
