# Web E2E — Notifications

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: none — Push delivery; manual checks only

## Web test cases (0)

## Sheet bugs (2)

- **NOTIF-01** Join-request and profile-request notifications open admin screens for non-admins — High · Web: Not checked, Android: Not checked
  - Steps: 1. As a non-admin member, tap a join-request or profile-request notification (push or in-app list).
  - Expected: The non-admin stays out of admin screens.
  - Actual: The admin screen opened for the non-admin.

- **NOTIF-02** Notifications list misses a recent row on first open; it appears after reopening — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Receive a notification (for example "Viewing rights received"). 2. Open the Notifications list.
  - Expected: The new row is listed on the first open.
  - Actual: The row was missing on the first open and appeared after the list was reopened.
