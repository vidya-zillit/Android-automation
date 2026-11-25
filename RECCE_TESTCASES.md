# Recce — test cases (web gap section)

Suites: `tests/recce/RecceTest.java` (list, filters, search, form, stops, personnel, picker,
detail, delete) and `RecceePaginationTest.java`. Run: `./run-and-report.sh --only RecceTest`
(IDE: `Zillit ▸ Recce`).

This file only records the cases added from the web E2E gap analysis
(`web-e2e/gaps/recce.md`). The older cases are documented in the suite's own display names.

## Web gap cases

| Case | Web id | What it checks | Last result |
|---|---|---|---|
| `crewPickerTicksWhileSearching` | RECCE-01 | Crew picker: with a search active, tapping a member ticks it, "1 chosen", Done adds one personnel row named after the member; reopened, the member is still ticked; untick + Done removes the row. Nothing is saved. | PASS (emulator-5554, 2026-11-24) |
| `blankLunchStopSurvivesEditPublish` | RECCE-02 | Publish `QA Recce Lunch <n>` (one crew member, default stop named "QA Named Stop"), Edit → add a stop, kind Lunch (verified on the field), place/time blank → Publish; the detail timeline must have a Lunch row. Timeline rows are read (the Stops tile does not count Lunch); the named stop proves the reader sees the timeline. The recce is deleted in `finally`. | FAIL, reproduces RECCE-02: timeline after publish = `[RENDEZVOUS · QA Named Stop]`, no Lunch row (emulator-5560, 2026-11-24). Caveat: the edit save itself is not read back separately |
| `exportOpensPdf` | RECCE-04 | Export PDF on a published recce reaches the in-app PDF viewer (`DrivePdfViewerActivity`) within 60 s with no "PDF failed" / "Couldn't export the PDF" toast. | PASS — opened DrivePdfViewerActivity (emulator-5554, 2026-11-24) |
| `disabledUseLooksDisabled` | RECCE-05 | Location picker before any point is chosen: `pickerUse` is disabled (passes), and its dominant fill colour is not the brand orange #F99300 ±12 (expected to fail: the tint is a plain colour, `fragment_recce_location_picker.xml:194`). | FAIL, real bug RECCE-05: enabled=false, fill #F99403 ≈ #F99300 (emulator-5554, 2026-11-24) |

Not automated:
- **RECCE-04, PDF body.** The "Map error: g.co/staticmaperror" text is inside the server-made PDF;
  Appium cannot read it. Manual/API step: download the attachment `POST /recce/:id/pdf` returns,
  extract the text, assert it has no `staticmaperror`.
- **RECCE-03** is web-only (Android draws a live `MapView`, not a static image).

Notes from the runs (2026-11-24):
- **Publishing needs a crew member.** Since Oct 2026 `RecceFormFragment.submit` refuses to publish
  without at least one person picked from project crew ("recce_publish_needs_crew"). The older
  cases that publish (`createAndPublish`, `publishAnExistingDraft`, `multipleStopsPersistToDetail`)
  never add one, so they skip/fail on that rule, not on an app bug. RECCE-02 adds one with
  `RecceFormPage.addFirstCrewMember()`; the same call would fix those three.
- Stop rows are taller than half the screen, so `setStopKind(index)` by visible index can hit the
  wrong row; and the kind dropdown's popup is not in the accessibility tree. RECCE-02 uses
  `setLastStopKind()`, which taps the item where it is drawn and reads the field back.
