# Web E2E — Script Pages

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (0)

## Sheet bugs (4)

- **SCR-01** An undated upload made right after a dated one is saved with the earlier date — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Upload a page script and pick a page date. 2. In the same session, upload another one without picking a date.
  - Expected: The second card reads "Page Date: Not Available".
  - Actual: The second upload kept the date picked for the first one.

- **SCR-02** Scene search is case-sensitive, so a scene typed in different case is not found — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Upload a page with scene "7QA0353". 2. Search scenes for "7qa0353".
  - Expected: The page is found whatever the case.
  - Actual: "No Data Found."

- **SCR-03** Publishing a page to Document Distribution files it in the wrong scene folder — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Upload a page with a scene number. 2. Publish it to Document Distribution. 3. Open Document Distribution > Pages.
  - Expected: The file sits under Pages/<scene number>.
  - Actual: The file landed directly under Pages, not in its scene folder.

- **SCR-04** Typing a scene number fast can leave the search on "No Data Found." for an existing page — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Upload a page with a long scene number. 2. Open scene search and type the full scene number quickly.
  - Expected: The page card shows once typing stops.
  - Actual: Once in 5 tries the search stayed on "No Data Found." (still at 8 s). Typing slowly finds it.
