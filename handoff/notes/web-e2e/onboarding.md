# Web E2E — Onboarding

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (0)

## Sheet bugs (1)

- **ONB-01** First and last names shorter than 3 characters are refused (real names like Li, Wu, Ng can't sign up) — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Choose Start a Project. 2. Type a 2-letter first name (e.g. "Al") or last name (e.g. "Li"), fill the rest and tap Submit.
  - Expected: Real 2-letter names are accepted (product decision).
  - Actual: Alert "First Name should be three characters Long." (or "Last Name …"); the form can't be submitted.
