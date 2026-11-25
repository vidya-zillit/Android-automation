# Settings / Admin Settings — web-gap cases

Gap analysis: `side-session-notes/.../web-e2e/gaps/settings.md` (13 missing web tests, proposed S-M1..S-M7).
Page object: `pages/settings/AdminRightsPage.java` (plus read-only use of `AdminSettingsPage`,
`UserManagementPage`, `DepartmentListingOrderPage`).

Run on an emulator (Vidya Testing, project admin) **while holding the Pixel's lock**: S-M2 and S-M4
change Vidya Pixel's rights / Admin Access and restore them in the case and again in `finally`.

## PermissionGridTest (`TestOrder.PERMISSION_GRID` = 215)

| # | Web | Case | Last result |
|---|---|---|---|
| S-M2.1 | SET-3 | Rights Grid row opens its "Select User" list (this build opens `PermissionGridUserList` directly; "Select Section" is commented out) | 04:30 FAIL (test expected Select Section) → fixed, rerun pending |
| S-M2.2 | SET-24 | Home → member → table names the member; every unit row has View / Post / Download | see run table |
| S-M2.3 | SET-25 | Home ⇄ Tools swaps the unit list; Home restores it | see run table |
| S-M2.4 | SET-26 | A greyed-out switch does not change when tapped (skips if none) | see run table |
| S-M2.5 | SET-30 | Grant View, survives reopen, revoke (restored) | see run table |
| S-M2.6 | SET-31 | Grant Post — View follows — revoke both (restored) | see run table |
| S-M2.7 | SET-32 | Grant Download — View follows — revoke (restored) | see run table |

## AdminSettingsWebGapTest (`TestOrder.ADMIN_SETTINGS_WEB_GAP` = 216)

| # | Web | Case | Last result |
|---|---|---|---|
| S-M1 | SET-1 | Admin Settings lists every core tile by name | PASS (emulator-5560, 04:33) |
| S-M3 | SET-8 | Approve User Profile opens the "Select User" queue; Back returns | opened OK; empty-view id fixed (`noDataFound`), rerun pending |
| S-M5 | SET-21 | Each User Management row has Status, Admin Access and the edit icon | PASS |
| S-M4 | SET-28/29 | Demote needs no dialog; No on the grant keeps non-admin (after reopen); Yes grants; role restored | see run table |
| S-M6 | SET-12 | Listing Order: Back without changes closes; after a drag it asks to save; No keeps the order | PASS |

## InviteUsersTest (existing suite)

| # | Web | Case | Last result |
|---|---|---|---|
| S-M7 | SET-35 | Tapping the code opens the Android share chooser; Back returns to SHARE PROJECT CODE (nothing shared) | PASS (emulator-5558, 00:57; chooser = com.android.intentresolver, preview carries the code) |

Role/right-changing cases (S-M2.5–2.7, S-M4) assume `ZILLIT_ROLE_CHANGES_OK=1`, which the runner sets
only while it holds the Pixel's lock (Vidya Pixel is the member whose rights/role change). S-M4 now also
covers web #27: while the member is a non-admin it deactivates them, checks the edit icon says "You can't
perform this action on disable user" and Admin Access is locked, then re-activates (restored in `finally`).

## Weak covered rows (added to AdminSettingsWebGapTest)

| # | Web | Case | Last result |
|---|---|---|---|
| 6 | SET-7 | Edit Project Name opens prefilled with the current project name | PASS |
| 7 | SET-23 | Change Profile closed without saving changes nothing | PASS |
| 8 | SET-34 | Edit My Profile → Preference → Edit Email & Phone opens (EditEmailAndPhoneActivity), Back returns | PASS |

## Not automatable / not done

- SET-02 Terms of Use: `ZillitHelpTest` TC9 was removed at the user's request; not restored.
- SET-19 "Share via Internal Email": web-only (Android hands the invite to the OS chooser — S-M7).
