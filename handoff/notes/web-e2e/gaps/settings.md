# Settings: web E2E vs Android Appium gap analysis

Web source: `web-e2e/settings.md` (36 tests, 3 sheet bugs).
Android suites: `tests/settings/` (AppPreferencesTest, EditMyProfileTest, InviteUsersTest, LinkedDeviceTest, ZillitHelpTest) and `tests/adminsettings/` (17 classes).
App source: `Zillit-Android/app/src/main`. Shorthands:
- `ADM/` = `java/com/zillit/zillitapp/bottomNav/settings/admin/`
- `ASA` = `ADM/views/AdminSettingsActivity.kt`
- `PG/` = `ADM/permissiongrid/`
- `STR` = `res/values/strings.xml`

Web test numbers (#N) follow the order of the web file (1 = "the hub renders the core tiles", 36 = "Web Preferences — changing the default Home unit").

## Summary

| Class | Web tests | Notes |
|---|---|---|
| COVERED | 22 | 5 weak (see notes) |
| MISSING | 13 | grouped into 7 proposed Android cases (S-M1 to S-M7) |
| WEB-ONLY | 1 | in-app "Share via Internal Email" |
| CONFLICT | 0 | |
| **Total** | **36** | |

The biggest gap: **per-user posting rights (View / Post / Download) have no Android automation at all.** On Android they are not in User Management; they live under Admin Settings → "User Viewing & Posting Rights Grid" (`ASA:492-498`, STR:1833). No class in the repo opens that screen. Web tests #3, #24-26 and #30-32 (7 tests) all land there.

Side finding: `UserManagementTest` TC9 makes the chosen user an admin and **never removes it**. TC13 only navigates. Every run leaves one more admin on the test project. That also breaks the web-style "pick a member who is not an admin" precondition for the next run.

---

## MISSING (prioritised)

Ids are `com.zillit.zillitapp:id/<id>`.

### S-M2. Posting Rights Grid: View / Post / Download for one user (web #3, #24, #25, #26, #30, #31, #32). P1

Android flow:
- Admin Settings row "User Viewing & Posting Rights Grid" (`ASA:492-498`) opens `PermissionGridMainActivity`, titled "Select Section" (STR:1358).
- That screen has three rows: `layHome`, `layTools` and `layDistribute` (`activity_permission_grid_main.xml:89,141,190`).
- `layHome` or `layTools` opens `PermissionGridUserList`, titled "Select User" (STR:436). Tapping a user opens `PermissionUserDetails`.
- `PermissionUserDetails` shows:
  - the user's name `tvName`
  - the switches `chkStatus` and `chkAdmin`
  - the section tabs `tvHome` and `tvTool` (`activity_permission_user_details.xml:159,175`)
  - the column headers `tvViewTitle`, `tvPostTitle` and `tvDownloadTitle`
  - a search box `et_search`, and the unit list `rvUnits`, where each row has `chkView`, `chkPost` and `chkDownload`

Proposed cases (one class, `PermissionGridTest`, chained). Run them on a member who is active and not an admin:
1. **Grid opens from Admin Settings.**
   - Steps: Settings → Admin Settings → tap "User Viewing & Posting Rights Grid".
   - Expected: "Select Section" shows Home and Tools rows, and the Admin Settings list (`groupTitleTv`) is gone. (Web #3 only checks that the screen changed. This case checks more.)
2. **Section → user → rights table.**
   - Steps: tap `layHome`, then the first non-admin user.
   - Expected: `tvName` equals that user. `rvUnits` has at least one row, and each row has `chkView`, `chkPost` and `chkDownload`. (Web #24)
3. **Home ⇄ Tools swaps the unit list.**
   - Steps: note the unit names in `rvUnits`, then tap `tvTool`.
   - Expected: `tvTool` turns selected (orange background, `PermissionUserDetails.kt:575-577`), and the unit names change to tool names such as Catering and Info. Tap `tvHome` and check the original list is back. (Web #25)
4. **A greyed-out switch is inert.**
   - Steps: find a row where `chkView`, `chkPost` or `chkDownload` has `enabled=false`. That happens when the server's `viewUpdatable`, `postUpdatable` or `downloadUpdatable` is false (`PG/adapter/holder/UserAccessHolder.kt:88-91`). Note its `checked` value and tap it.
   - Expected: `checked` does not change. If no row has a disabled switch, `assumeTrue` and skip. (Web #26; the web test also skipped.)
5. **Grant, then revoke View.**
   - Steps: find the first row with `chkView` off and enabled. Tap it and wait for the list to refresh (the success handler re-fetches, `PermissionUserDetails.kt:209-216`). Tap it again.
   - Expected: `chkView` is on after the first tap and off after the second, and both states survive leaving and re-entering the screen. (Web #30)
6. **Grant Post; View follows.**
   - Steps: find a row with `chkPost` off and enabled. Note `chkView`, then tap `chkPost`.
   - Expected: `chkPost` and `chkView` are both on after the refresh. That matches the web rule.
   - Note: the client only sends "post" (`UserAccessHolder.kt:46-49`), so View turning on is the server's doing. Assert it after the re-fetch, not straight after the tap.
   - Restore: tap `chkPost` off. If View was off before, tap `chkView` off as well. With Post still on, unticking View goes through "ViewPost" (`UserAccessHolder.kt:51-62`), which also clears Post. Assert that too. (Web #31)
7. **Grant, then revoke Download.**
   - Steps: find a row with `chkDownload` off and enabled, and tap it.
   - Expected: `chkDownload` is on, and View is turned on if it was off (`PermissionUserDetails.kt:508-523`, `REQ_USER_DOWNLOAD_VIEW_ACCESS`).
   - Restore: tap it off. (Web #32)
- Cross-check, optional and needs two devices: after step 6, device B opens that Home unit and gets "You have been given posting right for '%s'." (STR:1236).

### S-M1. Admin Settings lists every core tile (web #1). P1

- Gap: `AdminSettingsGroupsTest` checks the section headers, the counts, the order and search. It never checks that a named tile is present, so a tile dropped from `ASA` would pass.
- Steps: open Admin Settings, clear `searchEt`, then use `AdminSettingsPage.sweep()` to collect every row `nameTv`.
- Expected: these rows are present (exact strings from STR):
  - "User Management" (:1733)
  - "User Viewing & Posting Rights Grid" (:1833)
  - "Approve User Profile" (:338)
  - "Approve New User Requests" (:337)
  - "Create/Update Home Units." (:9388)
  - "Create New Department" (:340)
  - "Create New Designation" (:341)
  - "Customization of tools" (:943)
  - "Manage Tool Groups" (:1318)
  - "Edit Project Name" (:344)
  - "Pre-Approved Users" (:345)
  - "Set/View SOS Receivers" (:346)
  - "Watermark Logo of Company" (:347)
  - "Company Details" (:270)
  - "Change Department Listing Order for Crew List." (:2869). On an "Other" project type this row reads `set_department_priority_staff_list` instead (`ASA:675-677`).
  - "Delete Project" (:873), or "Stop Project Deletion" while a deletion is scheduled (`ASA:804`)
  - "Create Remote Shooting Units" (:342), only when the project offers it (`ASA:747`, conditional)
- Cheapest place for it: one more `@Order` in `AdminSettingsGroupsTest`.

### S-M4. User Management admin switch: Cancel, demote and restore (web #28, #29; also tightens #27). P1

Source:
- Turning `chkAdmin` on shows the "Alert" dialog with `admin_rights_message` (STR:2251; on an "Other" project, `admin_rights_message_for_other`, STR:2877).
- "No" resets the switch (`ADM/views/AddEditAdminsPage.kt:291-305`). "Yes" calls the API (:306-311).
- Turning it off calls the API straight away with no dialog (:313-316).

Cases:
1. **Cancel keeps the member a non-admin.**
   - Steps: pick an active user with `chkAdmin` off, tap `chkAdmin`, then tap No.
   - Expected: the dialog closes and `chkAdmin` is still off after `getProjectUsers` refreshes. Re-open User Management to be sure.
2. **Demote without a dialog.** Run this right after TC9 has granted admin.
   - Steps: tap `chkAdmin` on that user.
   - Expected: no dialog, and the switch settles off.
   - This also cleans up after TC9, so add it as TC9b before TC10, or move TC10's Contacts "(Admin)" check before the demote.
3. **A deactivated member's actions are gated.** This extends TC11.
   - Steps: while Status (`icCheck`) is off, tap the edit icon `changeProfile`.
   - Expected: the toast "You can't perform this action on disable user" (STR:169, `ADM/views/adapter/AddEditAdminUserVH.kt:80-87`), and Change Profile does not open. `chkAdmin` has `enabled=false` (`AddEditAdminUserVH.kt:108`).

### S-M3. Approve User Profile opens its queue (web #8). P2

- Source: the tile opens `ApproveNewUsersListActivity` with `ACTIVITY_KEY=PROFILE_CHANGE_APPROVE_REQUEST` (`ASA:284-298`). It is titled "Select User" (STR:436, `approvenewrequests/ApproveNewUsersListActivity.kt:137`), and its toolbar has `selectTv` = "Select All" (:111).
- Steps: Admin Settings → tap "Approve User Profile".
- Expected: the "Select User" screen opens, showing either a list or the empty view. Back returns to Admin Settings.
- `ApproveNewUserRequestTest` already drives the same activity for new requests, so its page object can be reused with the other extra.

### S-M5. Each User Management row has its controls (web #21). P2

- Android row (`res/layout/permission_user_item.xml`):
  - `nameTv`, `tvUserDesignation`
  - `changeProfile` (edit; hidden on Personal projects, `AddEditAdminUserVH.kt:52-56`)
  - `icCheck` under "Status" (STR:197)
  - `chkAdmin` under "Admin Access" (STR:1304)
- Steps: open User Management and look at the first three rows.
- Expected: each row has `icCheck`, `chkAdmin` and `changeProfile`.
- Web difference: the web row's "Posting Rights" button has no Android equivalent here. The only extra row action is `tvAddEditUser` "Allow users for chat" (STR:1732), shown only for users with `keepNamePrivate` (`AddEditAdminUserVH.kt:37-41`). Do not assert a Posting Rights button. That part of #21 is web-only (S-M2 covers rights).

### S-M6. Department listing order: back with and without changes (web #12, adapted). P3

- Web asserts that Save is disabled while nothing has changed. Android never disables `submitBtn` (`ADM/views/designations/DepartmentListPage.kt:315-322`). Instead, back compares the list (`areListsEqual`, :130-145).
- Cases:
  1. Open "Change Department Listing Order for Crew List." and press back without changes. Expected: it closes straight to Admin Settings.
  2. Drag a row (the long-press drag that `DepartmentListingOrderTest` already uses) and press back. Expected: the "Do you want to save changes?" dialog (STR:2219). Dismiss without saving, re-open the screen, and check the order is unchanged.
- `DepartmentListingOrderTest` covers saving a reorder but not this unsaved-changes guard.

### S-M7. Invite Users: tapping the code opens the share sheet with the invite text (web #35, adapted). P3

- Source: `invite_code_view` runs `shareText(...)` (`registration/view/ShareProjectCodeActivity.kt:108-128`). That opens the system chooser titled "Share Via" (`mediaHandler/imageeditor/utils/Extension.kt:589-595`). The text is "You have been invited to the project '<name>' …" (STR:1230) plus the project code.
- Steps: Settings → Invite Users, tap the code block (`invite_code_view`).
- Expected: the Android chooser opens (package `android` or `com.android.intentresolver`) with "Share Via". Its text preview, where the OS shows one, contains the project code from `InviteUsersTest` TC2. Close it with back and check you are still on "Share Project Code" (STR:330).
- The web's "copy, then paste into Bulletin" step is optional on Android. The chooser's "Copy" works on Android 13+. Then paste into `etMessage` and send. Treat that part as best-effort.

---

## CONFLICT

None. Two places where Android behaves differently from web, both handled above rather than as conflicts:
- **Listing order:** Android has no disabled Save. It uses the back guard instead (S-M6).
- **Posting Rights:** Android has no Posting Rights button in User Management. It uses the Grid instead (S-M2, S-M5).

---

## COVERED (web # → Android)

- #2 User Management tile lands on the list → `UserManagementTest` TC1 (`listsUsers`)
- #4 Approve New Request tile → `ApproveNewUserRequestTest` TC1
- #5 Create New Department form and list → `CreateNewDepartmentTest` TC1-5
- #6 Create New Designation dropdown and field → `CreateNewDesignationTest` TC1-4
- #7 Edit Project Name prefilled + Submit/Cancel → `EditProjectNameTest` TC1-6. **Weak:** no test asserts that the field is prefilled with the current project name. Add `assertEquals(toolbarProjectName, page.projectNameField())` before TC1 clears it.
- #9 Create/Update Home Unit list with search → `CreateUpdateHomeUnitTest` TC6
- #10 Customization of Tools list → `CustomizationOfToolsTest` TC1-2, `ToolsManagementTest` "Customization of tools opens…"
- #11 Manage Tool Groups → `ManageToolGroupsTest` TC1-10. Android has one screen (groups plus "NOT IN GROUP TOOLS", `ADM/views/ManageToolGroupsActivity.kt`), not two tabs, so the "exactly two tabs" part does not apply.
- #13 Watermark Logo → `WatermarkLogoTest` TC1-3
- #14 Pre-Approved Users form → `PreApprovedUsersTest` TC1-10
- #15 Company Details form → `CompanyDetailsTest` TC1-7
- #16 Set/View SOS Receivers → `SosReceiversTest` TC1
- #17 Remote Shooting Unit → `CreateRemoteShootingUnitTest` TC1-3
- #18 Delete Project 12/24/48 options, never deletes → `DeleteProjectTest` TC1-2 (also `CreateRemoteShootingUnitTest` TC5)
- #20 User Management page loads → `UserManagementTest` TC1
- #22 Search filters members → `UserManagementTest` TC2 (name), TC3 (designation)
- #23 Edit icon opens Change Profile → `UserManagementTest` TC4. **Weak:** closing without saving is never checked, because the suite always goes on to save.
- #27 Deactivate then reactivate → `UserManagementTest` TC11-12 (checked through Contacts). **Weak:** the gating of a deactivated user is not checked. See S-M4 case 3.
- #28 Promote with confirmation then demote → `UserManagementTest` TC9. **Weak:** promote only. It never demotes, and it leaves the user an admin. See S-M4 case 2.
- #33 Edit Profile first and last name → `EditMyProfileTest` TC13-15
- #34 Edit Preference phone + extra info + gender → `EditMyProfileTest` TC5-12 (unit, gender, Add More Details). The phone is covered only through the SOS path: `SosReceiversTest` TC13 opens the same `EditEmailAndPhoneActivity` with "Update Mobile Number". **Weak:** nothing opens "Edit Email & Phone" (STR:541) from Edit My Profile itself.
- #36 Default Home unit opens on entry → `AppPreferencesTest` TC6-7 (and it survives a restart)

## WEB-ONLY

- #19 Invite User → "Share via Internal Email" opens the in-app composer. Android has no in-app share menu. Tapping the code hands the text to the OS chooser (`ShareProjectCodeActivity.kt:109-128` → `Extension.kt:589-595`), and STR has no "internal email" string. The adapted case is S-M7.

## Sheet bugs vs Android

- **SET-01** The success alert closes by itself. iOS-specific: Android shows a snackbar, not an alert. `EditMyProfileTest` TC11 already waits for the "Profile updated" snackbar.
- **SET-02** Privacy Policy and Terms Of Use are blank.
  - Privacy Policy: COVERED by `ZillitHelpTest` TC8 (`checkWebPage`).
  - Terms of Use: **MISSING**. TC9 was removed "at request" (`ZillitHelpTest.java:229-230`), although the row is still listed in `ZillitHelpPage.java:52`. Restoring TC9 with the same `checkWebPage` pattern would catch a blank page.
- **SET-03** The notification setting opens the iOS Settings root. iOS-only. Android manages notifications in the app (`AppPreferencesPage.kt:628`, covered by `AppPreferencesTest` TC8-10).
