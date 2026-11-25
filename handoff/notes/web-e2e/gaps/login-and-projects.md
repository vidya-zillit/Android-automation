# Login & Projects: web E2E vs Android Appium gap analysis

Web source: `web-e2e/login-and-projects.md` (5 tests, 0 sheet bugs).
Android suites: `tests/project/` (ProjectListTest, CreateProjectValidationTest, CreateProjectSubTypeTest, CreateProjectE2ETest, JoinProjectTest, JoinProjectE2ETest, ProjectFlowE2ETest).
App source: `Zillit-Android/app/src/main/java/com/zillit/zillitapp/registration/view/`.

## Summary

| Class | Web tests |
|---|---|
| COVERED | 4 |
| MISSING | 0 |
| WEB-ONLY | 0 |
| CONFLICT | 1 |
| **Total** | **5** |

Android is ahead of web here. It also covers field validation, sub-types, the language and email errors, and Join.

---

## CONFLICT

### C1. "Submit stays disabled until the email is OTP-verified" (web #5) vs `CreateProjectValidationTest` "Valid but unverified email blocks submission" (Order 18)

- **Web asserts:** with every field filled but the email unverified, Submit is disabled.
- **Android test asserts:** Submit can be tapped, and tapping it shows the snackbar `Messages.EMAIL_NOT_VERIFIED`.
- **Android source:**
  - `StartProjectActivity.kt:453-454` validates on tap: `confirmationCode.isEmpty() && !emailVerified` → `showSnackBar(R.string.txt_enter_email_code)`.
  - The string is "Please click Verify button to authenticate email." (strings.xml:3034).
  - Submit is not disabled.
- **Verdict:** the Android test is right for Android. Do not port the web's "disabled" assertion.
- **Small fix:** `Messages.EMAIL_NOT_VERIFIED` (`core/Messages.java:23`) has no trailing full stop, but the app string does. This is fine while `textShown` matches on "contains". It would break if it ever switched to exact match.

---

## COVERED (web # → Android)

- #1 Signed-in session lands on the project list with at least 1 project → `ProjectListTest` "Project list shows projects, search and Start/Join actions"
- #2 Search filters the project list → `ProjectListTest` "Search filters the project list by name", plus the non-match and clear cases
- #3 Start Project opens with the full form, and Cancel closes it → `CreateProjectValidationTest` "Create-project form renders all fields down to the Submit button". Android opens a screen, not a dialog, so Cancel is back. The back step is not asserted (minor).
- #4 Entertainment Industry reveals Sub Type, which offers "Feature" → `CreateProjectValidationTest` "Sub-type selected but no language…" (selects "Feature", line 182), `CreateProjectSubTypeTest` "Entertainment project without a sub-type shows sub-type error"

## MISSING

None.

## WEB-ONLY

None.
