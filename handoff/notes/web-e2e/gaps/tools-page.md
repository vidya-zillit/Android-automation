# Tools page: web E2E vs Android Appium gap analysis

Web source: `web-e2e/tools-page.md` (9 tests, 0 sheet bugs).
Android suites:
- `tests/tools/ToolsManagementTest.java` (6 cases)
- `tests/adminsettings/ManageToolGroupsTest.java` (10 cases)
- `tests/adminsettings/CustomizationOfToolsTest.java`
- the tool-opening path in `tests/tools/ToolChatBaseTest.java`

Page object: `pages/common/ToolsPage.java`.
App source: `java/com/zillit/zillitapp/bottomNav/tools/Tools.kt` (shortened to `Tools.kt` below).

How Android differs:
- Android has no drag-to-group "organize mode" on the Tools tab. The Tools tab has:
  - a per-user **Reorder** sheet for the group order (`reorderBtn` → `ReorderToolGroupsBottomSheet`, Tools.kt:1717-1725, 1755)
  - an admin-only **Customization** banner (`toolsCustomization`, Tools.kt:477, 489)
- Moving tools between groups, renaming and creating groups all happen in Admin Settings → Manage Tool Groups (`ManageToolGroupsActivity`).

## Summary

| Class | Web tests |
|---|---|
| COVERED | 6 (2 weak) |
| MISSING | 2 |
| WEB-ONLY | 1 |
| CONFLICT | 0 |
| **Total** | **9** |

---

## MISSING

### T-M1. Every tool row has its info icon, also in dark theme, and the icon opens the info dialog (web #2)

- Source:
  - Every tool row is built with `suffixIcon = R.drawable.info` (Tools.kt:400-404).
  - Tapping the icon calls `openInfoDialog(suffixMessage, "<title>:", identifier)` (Tools.kt:1096-1098).
- Steps:
  1. Tools tab. Scroll the whole list (`ToolsPage.allToolNamesScrolled()`).
  2. For each row, check that the suffix icon is present: the same row's suffix `ImageView`. Take the id from the shared `CommonListAdapterModel` row layout.
  3. Tap the icon on the first row and check that a dialog opens whose title starts with that tool's name and ends with ":". Close it.
  4. Switch Settings → App Preferences → Theme → Dark (reuse `AppPreferencesTest` TC5), return to Tools, and repeat steps 2-3.
  5. Restore System Default.
- Expected: the number of icons equals the number of rows in both themes, and the info dialog shows the tool's description.

### T-M2. Only admins see Customization; everyone can reorder (web #4)

- Source:
  - The `toolsCustomization` banner is shown only when `userDetail?.isAdmin == true` (Tools.kt:477, 489).
  - `reorderBtn` has no admin gate, because group order is per-user (Tools.kt:1207 comment, 1755).
- Steps:
  1. Admin account: Tools tab. Customization is visible (already implied by `ToolsManagementTest` "Customization of tools opens…").
  2. Non-admin account: use the second device or a non-admin login, then open the Tools tab.
- Expected:
  - Non-admin: `toolsCustomization` is absent, and `reorderBtn` is present and opens the Reorder sheet.
  - Admin: both are present.
- Needs a non-admin account. Today the suite only `assumeTrue`s Customization ("admin only?"), so a non-admin regression or leak would go unnoticed.

---

## CONFLICT

None.

---

## COVERED (web # → Android)

- #1 Grouped tools with headings → `ToolsManagementTest` "Tools list loads: grouped sections with counts + tool rows". The web font-size comparison (heading smaller than page title) is a CSS check that Appium cannot see reliably, so it is not ported.
- #3 Tool cards are listed and reachable → `ToolsManagementTest` "Tools list loads" (`toolRowCount() > 0`)
- #5 Cards open their tool → `ToolChatBaseTest` (Catering, Info and Confidential Info are opened from the Tools tab, `ToolChatBaseTest.java:94-100`), and the per-tool suites (Drive, DocDist, Recce…)
- #7 Move a tool to another group, and it persists → `ManageToolGroupsTest` TC5-8 (remove from group → UNGROUPED on the Tools tab, move into the custom group → shown under it on the Tools tab)
- #8 Rename a group → `ManageToolGroupsTest` TC3-4 (empty name rejected, rename succeeds). **Weak:** the renamed heading is not checked on the Tools tab.
- #9 Create a group, which shows as empty → `ManageToolGroupsTest` TC1-2. **Weak:** it does not assert that the new group starts with 0 tools.

## WEB-ONLY

- #6 Organize mode turns the grid into a drag board with a "Drag a tool onto a group…" hint and a Done button. Android has no such mode on the Tools tab: there is no drag handle or hint string, and the only Tools-tab drag is the group order in the Reorder sheet (Tools.kt:1717). The Reorder sheet is covered by `ToolsManagementTest` "Reorder sheet opens…" and "Change group order…".
