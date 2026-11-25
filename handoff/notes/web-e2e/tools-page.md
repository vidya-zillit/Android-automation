# Web E2E — Tools page

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (9)

### Tools page presentation

- **shows grouped tools with headings that do not outshout the page @smoke** — passed (tools-organize.spec.js:37)
  - Where: Tools · account: iPhone XRR
  - Steps: Open Tools and wait for the grouped tool grid → Check that the “Film Tools” page title and the first group heading are shown → Compare their text sizes → Check that the group heading is smaller than the page title
  - Expected: Tools are shown in groups, and the group headings use a smaller font than the page title.

- **every tool card carries the info glyph, in dark mode too @smoke** — passed (tools-organize.spec.js:54)
  - Where: Tools, with the app in dark theme · account: iPhone XRR
  - Setup: Switches the app to dark theme before it loads.
  - Steps: Switch the app to dark theme → Open Tools and wait for the grouped tool grid → Count the tool cards → Count the info (i) icons on the cards → Check that every card has exactly one info icon
  - Expected: In dark theme every tool card still shows its info (i) icon.

- **every tool card is reachable as a link to its tool @smoke** — passed (tools-organize.spec.js:71)
  - Where: Tools · account: iPhone XRR
  - Steps: Open Tools and wait for the grouped tool grid → Check that there is at least one tool card → Check that the first card is visible
  - Expected: The Tools page shows at least one tool card. (Despite the title, the test does not open a card or check its link.)

### Organize mode

- **is offered to admins and hidden from everyone else @smoke** — passed (tools-organize.spec.js:82)
  - Where: Tools · account: iPhone XRR
  - Steps: Open Tools and wait for the grouped tool grid → Count the “Manage Tool Groups” buttons → Check that there is at most one → If there is none, check that no card shows a drag handle
  - Expected: There is at most one Manage Tool Groups button, and when it is missing (non-admin) no card offers a drag handle.

### Organize mode leaves the ordinary page alone

- **cards open their tool when not organizing @smoke** — passed (tools-organize.spec.js:203)
  - Where: Tools · account: iPhone XRR
  - Steps: Open Tools without turning on Manage Tool Groups → Click the first tool card → Check that the app leaves the Tools page for that tool
  - Expected: Outside organize mode, clicking a tool card opens its tool (the page moves away from Tools).

### Organize mode

- **turns the grid into a board, and turns it back** — notrun (tools-organize.spec.js:95)
  - Where: Tools → Manage Tool Groups · account: iPhone XRR
  - Steps: Open Tools and wait for the grouped tool grid → Click “Manage Tool Groups” → Check that the hint “Drag a tool onto a group to move it…” appears → Check that the tool cards show drag handles → Click Done → Check that the hint and every drag handle are gone
  - Expected: Manage Tool Groups turns the grid into a drag board with handles, and Done returns it to the normal Tools view.

- **moves a tool to another group, and it stays there after a reload** — notrun (tools-organize.spec.js:107)
  - Where: Tools → Manage Tool Groups · account: iPhone XRR
  - Steps: Open Tools and click “Manage Tool Groups” → Note the first tool card and the group it sits in → Drag that card onto a different group → Check that the tool now sits in the new group → Reload the page and open Tools again → Check that the tool is still in the new group → Click “Manage Tool Groups” and drag the tool back to its original group → Check that it is back where it started
  - Expected: A tool dragged to another group moves there, and the move is still in place after a reload.

- **renames a group from its heading** — notrun (tools-organize.spec.js:146)
  - Where: Tools → Manage Tool Groups · account: iPhone XRR
  - Steps: Open Tools and click “Manage Tool Groups” → Click the first renameable group heading → Replace its name with the same name plus “ QA” and press Enter → Check that a group with the new name is shown → Click Manage Tool Groups if needed, click the renamed heading again → Type the original name back and press Enter → Check that the group shows its original name
  - Expected: Clicking a group heading lets an admin rename the group, and the new name is shown as the group heading.

- **creates a group, which then shows as an empty target** — notrun (tools-organize.spec.js:180)
  - Where: Tools → Manage Tool Groups · account: iPhone XRR
  - Steps: Open Tools and click “Manage Tool Groups” → Click “Add new group” → Type “E2E Group <time>” and press Enter → Check that the new group appears on the board → Check that it shows the empty-group text “Drag a tool here”
  - Expected: A newly created group appears while organizing, as an empty drop target saying “Drag a tool here”.

## Sheet bugs (0)
