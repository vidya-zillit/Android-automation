# Web E2E report vs Android automation — gap summary (2026-11-23)

Source: the user's web automation report (`Zillit E2E Report`, generated 2026-11-17, 3,586 web tests).
Each web case was classified against our Android suites and the Android app source
(`Zillit-Android`): **covered**, **missing** (the feature exists on Android but no Android case),
**web-only** (absent on Android), or **conflict** (our case asserts the opposite of the web, or is wrong).
Full per-module reports with proposed Android cases (steps, expected, ids):
`side-session-notes/…/web-e2e/gaps/<module>.md`.

| Module | Web cases | Covered | Missing | Web-only | Conflict |
|---|---|---|---|---|---|
| Home | 165 | 30 | 102 | 30 | 3 |
| Calendar | 140 | 15 | 105 | 17 | 3 |
| Settings | 36 | 22 | 13 | 1 | 0 |
| Login & Projects | 5 | 4 | 0 | 0 | 1 |
| Tools page | 9 | 6 | 2 | 1 | 0 |
| Call Sheet (Home unit) | 9 | 4 | 5 | 0 | 0 |
| Document Distribution | 101 | 35 | 54 | 11 | 1 |
| Drive | 326 | 18 | 186 | 121 | 1 |
| Map | 220 | 70 | 108 | 35 | 7 |
| Info | 131 | 14 | 101 | 16 | 0 |
| Confidential Info | 125 | 14 | 93 | 18 | 0 |
| Catering | 148 | 24 | 85 | 34 | 5 |
| Email | 13 | 7 | 5 | 0 | 1 |
| C&C | 37 | 16 | 17 | 4 | 0 |
| Recce / External Users / Box / Schedule Full / Tools-Admin / Notifications (sheet bugs) | 18 | 0 | 17 | 1 | 0 |
| Production Report, Call Sheet Creation | — | added today (see their `_TESTCASES.md`) | | | |

## 1. Fix first — our existing tests are wrong or prove nothing

**Conflicts (our assertion vs the app):**
- `DocDistComposerTest` F-26 passes on known bug DD-24 (Cancel with typed content closes with no prompt / no draft) — should fail until fixed.
- `DriveSharedWithMeTest:232` expects Move for an Editor under Shared with me — the app hides it (`HomeFragment.kt:1459-1469`); flip it.
- Calendar: `deleteRecurringAll` expects rows to vanish (the app keeps "Cancelled" rows); `recurringWithoutEndRejected` never reaches its rule; `EmailCalendarTest` uses the old calendar's ids.
- Catering: `readByUser` should assert Read By is absent; the attachment picker is exactly Photo / Image Gallery / Audio / Document; catering units can be deleted.
- Map: C-18 can never run (Edit hidden); L-15/16/17 cannot check anything with the Location tool on.
- Kept as is (Android is right): Home 2,000-char limit and Reply hidden for view-only; Login's snackbar instead of a disabled Submit; Email disables Send instead of warning on no recipient; Map F-03 no result card.

**Tests that can never fail:**
`CcContactsTest.toggleFavourite`; `CcGroupManageTest.addMember` (passes on ≥ 1); `EmailTest.searchEmails`
(any end state); Sent-folder checks (pass whenever any mail is listed); Info `shareMessage` (fallback always
true) and `readByUser` (skips instead of failing); Drive Trash check (passes on an empty trash), the
folder-upload test (no assertion), "a new file starts with no one having access" (asserts nothing).

**Side effect:** `UserManagementTest` TC9 makes a user admin and never reverts it — one more admin per run.

## 2. Highest-value missing cases (bug present in the Android source too)

- **Notifications NOTIF-01 (High):** join / profile-request push taps open the admin approval screen with no admin check (`Extensions.kt:2535-2555`).
- **Document Distribution:** DD-23 (High) "Move here" pushed off screen with many folders; DD-11 search keeps hidden files ticked so Delete removes them; no case ever actually moves anything; DD-20 template overwrites typed text; DD-18 root search misses nested files; raw "Unable to resolve host" offline.
- **Calendar:** recurring edit (this / following / all); invitees accept/decline; 65 min shown as "1.5 hours"; card time ≠ detail time.
- **Home:** reply quote survives a unit switch; drafts per unit; Edit offered on another member's message; offline send → retry → delete. Call Sheet unit text messages never tested.
- **Info / Confidential Info:** view-only REQUEST bar; rights changed while open (Android's live listeners are commented out); search arrows don't re-search; deleted-message History; offline Retry.
- **Map:** delete a location type with its locations; zone guards (blank radius saves 30 mi, double tap makes two zones); filter chip counts stuck at 0.
- **Drive:** Trash restore / delete forever / empty; rename rules; Tags; Downloads; per-right controls; folder roles for a second user; share-link expiry and revoke.
- **Settings:** per-user View / Post / Download rights grid (Admin Settings → User Viewing & Posting Rights Grid).
- **Tools TOOLS-01:** Customization of Tools sorts case-sensitively. **Box BOX-01:** List search ignores event/note text; Box History untested.
- **C&C:** group name capped at 40 chars with no hint; no Groups empty state.
