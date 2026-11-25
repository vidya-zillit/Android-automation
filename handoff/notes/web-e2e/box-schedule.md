# Web E2E — Box Schedule

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: none — 

## Web test cases (0)

## Sheet bugs (3)

- **BOX-01** Search filters the calendar and the List view differently — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open Box Schedule. 2. Search for text that matches nothing (e.g. "zzqqnomatch"). 3. Switch between calendar and List view.
  - Expected: Both views show only matches; List view shows "No Results".
  - Actual: The two views used different filters, so one still showed unmatched events or notes.

- **BOX-02** Another member's History shows the title of a Personal Note — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. A adds a Personal Note. 2. On B, open History (all dates).
  - Expected: B sees no trace of A's Personal Note.
  - Actual: B's History lists "ADDED · NOTE … personal" with the note's title, though the note itself is hidden from B on the day page and in the list.

- **BOX-03** History shows a schedule's dates one day early — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Create a schedule for Nov 28 – Nov 30 (device in IST). 2. Open History.
  - Expected: The CHANGED · SCHEDULE card shows Nov 28 – Nov 30.
  - Actual: It shows Nov 27 – Nov 28 / 1 day(s) added.
