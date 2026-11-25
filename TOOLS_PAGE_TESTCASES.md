# Tools page and Customization of tools: test cases (web gap section)

Suites: `tests/tools/ToolsManagementTest.java` (Tools tab) and
`tests/adminsettings/CustomizationOfToolsTest.java` (Admin Settings → "Customization of tools",
the web's "Select Tools"). Gap files: `web-e2e/gaps/tools-page.md`, `web-e2e/gaps/tools-admin.md`.

The new cases are read-only. Run them one at a time so the destructive cases of either suite are
not needed:

    ./run-and-report.sh --only 'CustomizationOfToolsTest.listIsCaseInsensitiveAlphabetical'
    ./run-and-report.sh --only 'ToolsManagementTest.tickedToolsAllHaveTiles'
    ./run-and-report.sh --only 'ToolsManagementTest.everyRowHasInfoIconBothThemes'
    ./run-and-report.sh --only 'ToolsManagementTest.adminSeesCustomizationAndReorder'

## Web gap cases

| # | Web id | Case | Expected | Last result |
|---|---|---|---|---|
| CustomizationOfTools TC7 `listIsCaseInsensitiveAlphabetical` | TOOLS-01 | Read every `nameTv` on Customization of tools, in screen order | The list equals itself sorted with `CASE_INSENSITIVE_ORDER`; "Account Hub" above "AD Dashboard". Expected to fail: `ToolsEnableDisablePage.kt:130` sorts with case-sensitive `sortBy { it.title }` (fix: `sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })`) | **FAIL, reproduces TOOLS-01** 2026-11-24 (emulator-5554): 42 tools listed, "AD Dashboard" at 0, "Account Hub" at 1 |
| `tickedToolsAllHaveTiles` | TOOLS-02 | Collect the ticked tools on Customization (opened from the Tools banner, nothing changed), then every tile on Tools | Every ticked tool has a tile (prefix match either way: Tools shows "Production Report Creation" to admins). If Account Hub is ticked, its tile shows "Available on web only" / "…not available on mobile devices…", and OK returns to Tools | _pending_ |
| `everyRowHasInfoIconBothThemes` | tools-page #2 (T-M1) | Scroll the Tools list, checking each `rowRoot` for its `infoIv`; tap the first row's icon. Switch App Preferences → Theme to the other of Light/Dark and repeat; the original theme is restored in `finally` (the theme is a per-device SharedPref) | Every row has the icon in both themes; the icon opens a dialog titled "<tool>:" with OK | **PASS** 2026-11-24 (emulator-5554): 55/55 rows with the icon in the current theme and in Dark |
| `adminSeesCustomizationAndReorder` | tools-page #4 (T-M2), admin half | On Tools as admin | `toolsCustomization` and `reorderBtn` both shown; Reorder opens its sheet | **PASS** 2026-11-24 (emulator-5554) |

Not automatable today:
- **T-M2, non-admin half** (no Customization banner, Reorder still there). It needs a non-admin
  account. Both test accounts are project admins, and demoting one would change a real account's role.
- **tools-page #6 (organize mode)** is web-only: Android has no drag-to-group mode on the Tools tab.
