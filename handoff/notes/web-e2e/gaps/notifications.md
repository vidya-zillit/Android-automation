# Notifications: sheet bugs vs Android suite

Web input: `notifications.md` (0 web tests, 2 sheet bugs; the web side notes "push delivery; manual checks only").
Android suites: none. The bell list (`NotificationActivity`) is not opened by any test. `AppPreferencesTest` only touches the notification settings.
App source: `Zillit-Android/.../bottomNav/common/view/NotificationActivity.kt` and `utils/Extensions.kt`.

## Summary

| Bug | Title | Status | Android case / note |
|---|---|---|---|
| NOTIF-01 | Join-request and profile-request notifications open admin screens for non-admins | **MISSING** (push path), likely reproduces | The push route has no admin check. The in-app list route does not exist on Android: tapping a row does nothing |
| NOTIF-02 | The list misses a recent row on first open | **MISSING** | — |

Counts: 2 MISSING, 0 COVERED, 0 WEB-ONLY, 0 CONFLICT.

---

## MISSING, in full

### NOTIF-01: a non-admin is not taken into the admin approval screens

Facts from the source:
- **Push / deep-link path:** `Context.navigationHandler(...)` (Extensions.kt:2258), under `SETTINGS_NOTIFICATION_KEY`, sends both `PROFILE_CHANGE_ACTION_KEY` and `PROJECT_JOIN_USER_ACTION_KEY` straight to `ApproveNewUsersListActivity` (Extensions.kt:2535-2555). There is **no admin check** there, and `ApproveNewUsersListActivity` has no `isAdmin` guard either. **Android very likely has this bug.**
- **In-app list path:** `NotificationActivity`'s `onNotificationItemTap` is an empty body (NotificationActivity.kt:173-175), so tapping a row in the list goes nowhere on Android. The "in-app list" half of the sheet bug cannot happen. Pin that too, so that wiring it up later without a guard gets noticed.

**Proposed case:** `NotificationsTwoUserTest.NT_01_nonAdminRequestPushStaysOut`. Two devices, set up like `homechat/TwoUserChatTest.java`; B is a **non-admin** member.

**Setup, one of:**
- (a) Use an account that was admin when a join request arrived and has since been demoted (`UserManagementTest` can demote).
- (b) Have a third account send a join or profile request, and confirm with the backend whether non-admins receive it. If they never do, the bug only shows with a stale push from (a).

**Steps (B):**
1. `driver.openNotifications()`.
2. Tap the Zillit push whose text mentions the join or profile request.
3. Wait 5 s.

**Expected:**
- `ApproveNewUsersListActivity` is **not** in front (`driver.currentActivity()` does not end with `ApproveNewUsersListActivity`).
- The app shows the project home or a "you can't perform this action" message instead.

**In-app part (single device, B):**
1. Tap the toolbar logo `imgLogo` (BottomNavigationActivity.kt:544-546) to open the list (`rvHelper`).
2. Tap the request row.

**Expected:** the screen does not change. This pins the current no-op.

**Severity:** High. This one is worth doing first, even as a manual checklist step, if B cannot be automated.

### NOTIF-02: a recent notification is listed on the first open

`NotificationActivity.onCreate` calls `viewModel.getNotifications()` once (NotificationActivity.kt:57-62). There is a reload hook at :166 (`if (!binding.progress.isVisible) callApi()`).

**Proposed case:** `NotificationsTwoUserTest.NT_02_newRowOnFirstOpen`

**Steps:**
1. On A, do something that notifies B, for example grant B viewing rights on a tool ("Viewing rights received"), or any action the approval suites already trigger (`CallSheetApprovalTest`, `ProductionReportApprovalTest`).
2. On B, wait for the push in `openNotifications()`, then close the shade.
3. Tap `imgLogo` **once**. Wait for `progress` / `linearProgress` to go.
4. Read the first 3 rows of `rvHelper`.

**Expected:**
- The new notification's text is among them on this **first** open.
- `noDataFound` is not shown.
- Do not reopen before asserting; reopening is what hides the bug.

**Cleanup:** delete the row through its delete action and confirm ("Are you sure you want to delete this notification?"). Then revoke the right on A.
