# App boot: web E2E vs Android Appium gap analysis

Web source: `web-e2e/app-boot.md` (3 tests, 0 sheet bugs).
Android: no dedicated suite. Signed-out start is exercised by every suite with `requiresLoggedOutStart()` (for example `JoinProjectTest`, `CreateProjectValidationTest`) through `BaseTest.settleLanding()`.
App source: `Zillit-Android/app/src/main/java/com/zillit/zillitapp/registration/view/StartAndJoinActivity.kt`, `res/layout/join_qr_code_layout.xml`.

## Summary

| Class | Web tests |
|---|---|
| COVERED | 1 (implicit) |
| MISSING | 1 |
| WEB-ONLY | 1 |
| CONFLICT | 0 |
| **Total** | **3** |

---

## MISSING

### B-M1. A cold start leaves no crash and no fatal error (web #3)

- Gap: nothing asserts that the app started cleanly. A crash during launch shows up only as a vague "No known landing screen appeared".
- Steps:
  1. `terminateApp`, clear the logcat buffer (`driver.manage().logs().get("logcat")` once, to drain it), then `activateApp`.
  2. Wait for the landing screen: Consent, Start & Join, or the project list.
  3. Read the logcat entries again.
- Expected: no entry contains `FATAL EXCEPTION` or `AndroidRuntime: Process: com.zillit.zillitapp`. The app stays in the foreground (`queryAppState == RUNNING_IN_FOREGROUND`).
- Run it signed out, and once more signed in.
- Related gap already recorded in `MODULE_COVERAGE_GAP.md`: the first screen after a fresh install is the Consent screen ("Your privacy choices", `consent/ConsentActivity`), which `settleLanding()` does not recognise. A boot test should assert that screen and accept or decline it explicitly.

---

## CONFLICT

None.

---

## COVERED

- #1 A signed-out visitor is sent to Login → implicit. With no session the app opens on Start & Join, which every logged-out suite needs (`settleLanding()`), and `JoinProjectTest` "Back from the join screen returns to Start & Join" asserts that screen. Weak: no test asserts it as its own case, and the Consent screen in front of it is unhandled (see B-M1).

## WEB-ONLY

- #2 The Login page shows the "Visit Corporate Web Site" link. On Android, "Visit Corporate Website" (`visit_corporate_website`, strings.xml:510) exists only inside the QR "Link a Device" sheet (`join_qr_code_layout.xml:204-215`, opened from `joinUsingQrBtn`). That button is shown only on tablets (`StartAndJoinActivity.kt:84-86`, `isVisible = AppHelper.isTablet(...)`). On phones, which are the test devices, it is absent. A tablet-only case is possible but low value.
