# Tools / Admin: sheet bugs vs Android suite

Web input: `tools-admin.md` (0 web tests, 2 sheet bugs).
Android suites:
- `tests/tools/ToolsManagementTest.java`
- `tests/adminsettings/CustomizationOfToolsTest.java` and `ManageToolGroupsTest.java`, plus 15 other admin-settings classes that do not touch these bugs.

Android name: the web's "Select Tools" is **Admin Settings → "Customization of tools"** (`ToolsEnableDisablePage`; header string `project_tools_enable_disable`). The same screen is also reached from the Tools tab banner.

## Summary

| Bug | Title | Status | Android case / note |
|---|---|---|---|
| TOOLS-01 | Select Tools sorts upper-case names first ("AD Dashboard" above "Account Hub") | **MISSING**, reproduces | Android sorts with `sortBy { it.title }`, which is case-sensitive. No test reads the order |
| TOOLS-02 | Account Hub is ticked but has no tile on the admin's Tools landing | **MISSING** | CustomizationOfTools TC4 and TC6 check "disabled tools gone, re-enabled tools back", but not "ticked implies tile" for Account Hub specifically |

Counts: 2 MISSING, 0 COVERED, 0 WEB-ONLY, 0 CONFLICT.

---

## MISSING, in full

### TOOLS-01: Customization of tools is sorted A to Z, ignoring case

- `ToolsEnableDisablePage` ends with `toolsList.sortBy { it.title }` (bottomNav/settings/activity/ToolsEnableDisablePage.kt:130). Kotlin `String` comparison is case-sensitive, so "AD Dashboard" (`adui_d_title_ad_dashboard`) sorts above "Account Hub" (`ah_account_hub`).
- **The bug reproduces on Android.**

**Proposed case:** `CustomizationOfToolsTest.TC7_listIsCaseInsensitiveAlphabetical`

**Steps:**
1. Admin Settings → "Customization of tools".
2. Scroll the list to the end, collecting every `nameTv` in screen order. De-duplicate while scrolling.

**Expected:**
- The list equals itself sorted with `String.CASE_INSENSITIVE_ORDER`.
- In particular, `indexOf("Account Hub") < indexOf("AD Dashboard")` when both are present.

**Report:** this **fails today**. Raise it as an Android bug with the file and line above. The fix is `sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })`.

### TOOLS-02: every ticked tool the admin can view has a tile on Tools

What the Android code does:
- The Tools landing drops a tool only when `viewAccess == false`. Invoices are also dropped for accountants (bottomNav/tools/Tools.kt:340-356).
- Account Hub **does** get a tile on Android. Tapping it shows a dialog, "Available on web only" / "Account Hub is currently not available on mobile devices…" (Tools.kt:722-736; strings.xml:5674-5675), instead of opening the hub.

**Proposed case:** `ToolsManagementTest.tickedToolsAllHaveTiles`

**Steps:**
1. As admin, open "Customization of tools". Collect each `nameTv` whose tick is checked (the enabled set E).
2. Go back to the Tools tab. Scroll through every group, collecting the `nameTv` of each `item_tools_section_row` (set T). Use the tool search as a fallback for long lists.

**Expected:**
- Every name in E is in T. Allow a documented list of exceptions: tools hidden by `viewAccess = false` for this admin, and Invoices for accountants.
- In particular, if "Account Hub" is ticked, a tile named "Account Hub" exists.
- Tapping it shows the "Available on web only" dialog with OK, and OK returns to Tools.

**Note:** the enable/disable round trip is already exercised by `ToolsManagementTest`'s "Disable a tool → it disappears…" case and by CustomizationOfTools TC3 to TC6. This new case is read-only.
