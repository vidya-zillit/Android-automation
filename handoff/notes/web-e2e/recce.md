# Web E2E — Recce

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (0)

## Sheet bugs (5)

- **RECCE-01** Crew picker: tapping a crew member while a search is active does not tick them — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open Recce, tap +, tap the crew field. 2. Type part of a crew member's name in the search bar. 3. Tap that crew member.
  - Expected: The row ticks, a personnel row is added and the bottom bar counts "1 chosen".
  - Actual: The row does not tick while the search filter is active. After cancelling the search, tapping the same person ticks them.

- **RECCE-02** A blank Lunch stop added in Edit is dropped when the recce is published — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open a published recce, tap Edit. 2. Tap Add stop and set the new stop's kind to Lunch. Leave its other fields empty. 3. Tap Publish recce.
  - Expected: The detail page shows the Lunch stop (fork icon, not numbered), as the test plan expects.
  - Actual: The Lunch stop is missing after saving. The server-generated PDF has no Lunch stop either.

- **RECCE-03** Recce detail page becomes wider than the screen once the Route map loads — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open a published recce that has stops. 2. Wait for the Route map image to load.
  - Expected: The page keeps the screen width.
  - Actual: When the map snapshot loads, the whole detail page becomes wider than the screen.

- **RECCE-04** Server-generated recce PDF shows "Map error: g.co/staticmaperror" instead of the route map — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Publish a recce with stops. 2. Open its PDF.
  - Expected: The PDF shows the route map.
  - Actual: The map area reads "Map error: g.co/staticmaperror".

- **RECCE-05** Disabled "Use" button in the location picker stays orange — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open the location picker in the recce form. 2. Before choosing a place, look at the Use button.
  - Expected: A disabled Use button looks disabled (greyed out).
  - Actual: It stays orange, although tapping it does nothing.
