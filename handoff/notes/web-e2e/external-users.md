# Web E2E — External Users

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (0)

## Sheet bugs (2)

- **EXT-01** Reserved Additional Info titles are accepted in another case, and the e-mail field auto-capitalises — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open the create external user form. 2. Add an Additional Info title "Full Name" or "e-mail". 3. Type an e-mail; then type an invalid one and Submit.
  - Expected: Reserved titles are refused in any case; the e-mail stays lower case; an invalid e-mail gets "Email is not valid…".
  - Actual: Reserved titles were only refused in one exact case, the e-mail field capitalised the first letter, and several errors showed together.

- **EXT-02** The back chevron on the external user form needs two taps — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open the create external user form. 2. Tap the back chevron once.
  - Expected: One tap goes back.
  - Actual: It took two taps.
