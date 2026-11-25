# Web E2E — Tool Windows

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: light — Classic ⇄ Windowed switch

## Web test cases (1)

### 

- **sidebar highlights C&C after Classic → Windowed on C&C @view-toggle** — passed (view-toggle.spec.js:27)
  - Where: Home (sidebar), on a project that offers the Classic view / Windowed view switch · account: iPhone XRR
  - Steps: Enter the project and open Home → If the sidebar shows “Classic view”, click it so the app is in the classic (full-page) layout → Click C&C in the sidebar → Check that C&C opens and is the highlighted sidebar item → Click “Windowed view” in the sidebar → Check that a C&C window tab appears in the window strip → Check that the sidebar still highlights C&C and not Home → Click “Classic view” again → Check that C&C opens full-page and stays highlighted
  - Expected: Switching from Classic to Windowed view while on C&C keeps C&C highlighted in the sidebar (never Home), and switching back lands on C&C with the same highlight.

## Sheet bugs (0)
