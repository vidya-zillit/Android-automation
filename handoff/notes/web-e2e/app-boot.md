# Web E2E — App boot

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (3)

### app boot (unauthenticated)

- **redirects to /login @smoke** — passed (smoke.spec.js:6)
  - Where: The app’s home address, opened in a browser that is not signed in · account: none (signed-out visitor)
  - Steps: Open the Zillit web app in a fresh browser without signing in → Check that the app sends you to the Login page
  - Expected: A signed-out visitor is sent to the Login page instead of the app.

- **login page renders branding and corporate link @smoke** — passed (smoke.spec.js:11)
  - Where: Login page, opened in a browser that is not signed in · account: none (signed-out visitor)
  - Steps: Open the Login page without signing in → Check that the “Visit Corporate Web Site” link to Zillit’s corporate website is shown
  - Expected: The Login page renders and shows the “Visit Corporate Web Site” link.

- **no page crash: root did not render an error boundary @smoke** — passed (smoke.spec.js:18)
  - Where: Login page, opened in a browser that is not signed in · account: none (signed-out visitor)
  - Steps: Open the browser’s developer console → Open the Login page without signing in → Wait until the page has finished loading → Check that no uncaught script error was reported while it loaded
  - Expected: The Login page loads without a single uncaught page error (no crash screen).

## Sheet bugs (0)
