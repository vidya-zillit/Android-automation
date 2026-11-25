# Web E2E — Login & Projects

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: moderate — Login redirect, projects list, Start Project dialog

## Web test cases (5)

### projects page (authenticated)

- **session lands on /projects with a populated project list @smoke** — passed (projects.spec.js:6)
  - Where: The app’s home address, signed in (before entering any project) · account: iPhone XRR
  - Steps: Open the Zillit web app while signed in → Check that you land on the Projects page with the “Projects” heading → Wait for the project list to load → Check that the list shows at least one project
  - Expected: A signed-in user lands on the Projects page and sees a list with at least one project.

- **search filters the project list @smoke** — passed (projects.spec.js:17)
  - Where: Projects page (the project list shown after signing in) · account: iPhone XRR
  - Steps: Open the Projects page → Wait until the project list stops changing and count the projects → Read the name of the first project in the list → Type the first 8 characters of that name in “Search projects...” → Check that the first project left in the list contains the typed text (case ignored) → Check that the list has no more projects than before the search
  - Expected: Searching filters the project list: the top result matches the search text and the list does not grow.

### start project dialog (authenticated)

- **opens with the full form and closes on Cancel** — passed (project-creation.spec.js:27)
  - Where: Projects page → Start Project · account: iPhone XRR
  - Steps: Open the Projects page → Click “Start Project” → Check that the dialog shows the first name, last name, project name and email fields → Click Cancel → Check that the dialog closes
  - Expected: The Start Project dialog opens with its full form and closes when Cancel is pressed.

- **selecting Entertainment Industry reveals the Sub Type field** — passed (project-creation.spec.js:37)
  - Where: Projects page → Start Project · account: iPhone XRR
  - Steps: Open the Projects page → Click “Start Project” → Open the Project Type dropdown → Choose the Entertainment Industry option → Check that a “Project Sub Type” field appears → Open the Project Sub Type dropdown → Check that it offers “Feature”
  - Expected: Choosing Entertainment Industry as the project type reveals the Project Sub Type field, whose options include Feature.

- **submit stays disabled until the email is OTP-verified** — passed (project-creation.spec.js:66)
  - Where: Projects page → Start Project · account: iPhone XRR
  - Steps: Open the Projects page and click “Start Project” → Type “E2E” as first name and “Test” as last name → Pick the first option in Project Type, then the first option in the next dropdown → Type a unique project name such as “e2e-smoke-<time>” → Type “e2e@example.com” as the email → Tick the terms checkbox → Check that the Verify button is enabled → Check that the Submit button is still disabled
  - Expected: With every field filled but the email not yet verified by one-time code, Verify is available and Submit stays disabled. Nothing is submitted, so no code is emailed and no project is created.

## Sheet bugs (0)
