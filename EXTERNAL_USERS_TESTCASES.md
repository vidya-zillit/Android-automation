# External Users — test cases (web gap section)

Suites: `tests/externalusers/ExternalUserTest.java` (EU-01..43) and
`ExternalUserDropdownProbeTest.java`. Run: `./run-and-report.sh --only ExternalUserTest`
(or `--external-users`).

This file only records the cases added from the web E2E gap analysis
(`web-e2e/gaps/external-users.md`). The older cases are documented in the suite's own display
names.

## Web gap cases

| Case | Web id | What it checks | Last result |
|---|---|---|---|
| EU-43 `backChevronOneTap` | EXT-02 (+ EXT-01b) | One tap on the toolbar back chevron (`toolbar → backBtn`, not system back) leaves ADD EXTERNAL USER, USER DETAILS and the update form for the listing. Self-contained: saves its own `ZL AUTO Back <n>` Vendor (`zlauto<n>@example.com`), checks the detail screen shows that email exactly in lower case (EXT-01b), and deletes the user in `finally`. | PASS — one tap each on all three screens; email lower case (emulator-5554, 2026-11-24) |
| EU-42 `reservedInfoTitlesRefused` | EXT-01a | "Add More Information" refuses titles that repeat the form's fields: "Full Name", "full name", "FULL NAME", "e-mail", "Email". Expected to fail: the dialog only checks for empty title/description (`UIExtentions.kt` `showDialogWithMultipleEditBox`). Nothing is submitted. | FAIL, real bug EXT-01: all five titles accepted (emulator-5554, 2026-11-24) |
| EU-33 `detailIsReadOnly` (extended) | EXT-01b | In the full-suite ladder: the email EU-30 saved comes back on the detail screen exactly, all lower case. Standalone runs of the same check go through EU-43. | Verified via EU-43: PASS (emulator-5554, 2026-11-24); the full-suite run of EU-33 was on the Pixel and is void |
| — | EXT-01c | One error at a time: already covered by EU-20..28 (EU-23 pins "Please Enter Valid Email."). | covered |

Manual step (EXT-01b): Appium types with setText, so it cannot show whether the soft keyboard
capitalises the first letter. Tap the email field and type with Gboard by hand; the field is
`TYPE_TEXT_VARIATION_EMAIL_ADDRESS` with no `TYPE_CLASS_TEXT` (AddUpdateExternalUserPage.kt:195-198).

Found while writing EU-43 (not a web case): the **update form's header reads "ADD EXTERNAL USER"**.
`onCreate` calls `initView()` (which picks the title from `isEdit`) before `handleIntentData()`
sets `isEdit = true` (AddUpdateExternalUserPage.kt:173-180, :190-192, :361). EU-43 therefore
recognises the update form by its "Update" button, not its header.
