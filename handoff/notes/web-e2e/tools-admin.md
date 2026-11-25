# Web E2E — Tools/Admin

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (0)

## Sheet bugs (2)

- **TOOLS-01** Select Tools sorts upper-case names first ("AD Dashboard" above "Account Hub") — Low · Web: Not checked, Android: Not checked
  - Steps: 1. As an admin, open Select Tools. 2. Scroll the list.
  - Expected: Tools are sorted A to Z ignoring upper/lower case.
  - Actual: "AD Dashboard" is listed above "Account Hub": the sort is case-sensitive.

- **TOOLS-02** Account Hub is ticked in Select Tools but does not appear on the admin's Tools landing — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. As an admin, open Select Tools and check that Account Hub is ticked. 2. Go back to the Tools landing.
  - Expected: Every ticked tool the admin can view shows on the landing.
  - Actual: Account Hub is ticked but has no tile on the landing.
