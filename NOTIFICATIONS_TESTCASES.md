# Notifications: test cases (web gap section)

Suite: `tests/notifications/NotificationsTest.java` (`@Order(TestOrder.NOTIFICATIONS)` = 190), page
`pages/notifications/NotificationsPage.java`. Gap file: `web-e2e/gaps/notifications.md` (NOTIF-01, NOTIF-02).
Run: `./run-and-report.sh --only NotificationsTest`. Single device, read-only (nothing deleted or sent).

App facts (Zillit-Android-devbuild):
- The list is `NotificationActivity`, opened by the bottom-nav toolbar logo `imgLogo`
  (BottomNavigationActivity.kt:553-555). Ids: `rvHelper`, `progress`, `linearProgress`, `noDataFound`.
  Rows (received_notification_item.xml): `mainView`, `sos_notification_message`,
  `sos_notification_time`, `sosNotificationDelete`. Only `is_global` notifications are listed
  (NotificationAdapter.filterList).
- A row tap calls `onNotificationItemTap`, whose body is empty (NotificationActivity.kt:173-175).
- A push tap goes through `NotificationHandlerActivity` (not exported) → `navigationHandler`
  (Extensions.kt:2264). SETTINGS_NOTIFICATION_KEY + PROFILE_CHANGE / PROJECT_JOIN_USER open
  `ApproveNewUsersListActivity` with no admin check (Extensions.kt:2541-2561).

## Web gap cases

| # | Web id | Case | Expected | Last result |
|---|---|---|---|---|
| NT-02 `newestRowsOnFirstOpen` | NOTIF-02 | Open the list once and read the top rows. Reopen it and read them again (a third read rules out a push that landed in between). | The first open already shows the rows a reopen shows; no empty state on the first open | **PASS** 2026-11-24 (emulator-5554): first and second open both list the same 5 rows ("Vidya Pixel has accepted the event." ×4, the welcome row) |
| NT-01a `rowTapStaysOnList` | NOTIF-01 (in-app half) | Tap a row (a join/profile/request row when one is listed, else the first). | Still on `NotificationActivity`, never `ApproveNewUsersListActivity`. Pins today's no-op, so wiring the tap later without an admin check gets noticed | **PASS** 2026-11-24 (emulator-5554): still on NotificationActivity after the tap |
| NT-01b `nonAdminRequestPushStaysOut` | NOTIF-01 (push half) | As a non-admin, tap a join/profile-request push in the shade, wait 5 s. | `ApproveNewUsersListActivity` is not in front | **SKIP** 2026-11-24: the user is a project admin (Customization banner shown). Not automatable with the current accounts, see below |

Not automatable today:
- **NOTIF-01, push half.** It needs a non-admin who receives a join or profile-change request push.
  Both test accounts (Vidya Testing, Vidya Pixel) are project admins. Making one a non-admin means
  changing a real account's role, and a join request needs a third account (a stranger).
  `NotificationHandlerActivity` is not exported, so the push tap cannot be faked with `am start`
  either. NT-01b checks the role first and skips with that reason; it runs as written once a
  non-admin account exists. Manual step: log in as a non-admin who was an admin when a request
  arrived, tap the push, expect the project home rather than "Approve New Users".
- **NOTIF-02, two-device form** (A does something that notifies B, then B opens the list once).
  A safe trigger exists: calendar event acceptance produces listed rows ("Vidya Pixel has accepted
  the event."). But it needs the Pixel (Vidya Pixel), whose app is held behind its app-lock prompt
  (2026-11-24), so the two-device form is not written. NT-02 covers the same bug on one device:
  if the first open misses a recent row, the reopen shows it and the case fails.
