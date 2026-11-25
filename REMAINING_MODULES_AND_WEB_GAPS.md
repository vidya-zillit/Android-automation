# Remaining modules and web-case gaps (2026-11-23)

Sources: `MODULE_AUTOMATION_LIST.md` (37 tools on the project) and the web E2E report
(`Zillit E2E Report`, 2026-11-17), compared module by module. The per-module comparisons, with
proposed Android cases, are in `side-session-notes/…/web-e2e/gaps/<module>.md`.

## 1. Modules with no automation yet (20 of 37)

| Group | Module | Web material we can reuse |
|---|---|---|
| Accounts / Payroll | Asset Register | Account Hub: 1,931 web tests cover this family |
| | Budget Builder | none |
| | Cost Report | Account Hub |
| | Deal Memo | Account Hub, plus the legacy Deal Memo notes |
| | Message Accounts | none (a chat tool: reuses `ToolChatBaseTest`) |
| | Payroll | Account Hub |
| | Petty Cash Expenses | Account Hub |
| | Production Expense Cards | Account Hub |
| | Purchase Order | Account Hub, plus the legacy PO notes |
| | Talent Deals | Account Hub |
| | Time Card | Account Hub |
| ADs | AD Dashboard | none |
| | Create Shooting Order | none |
| | Supporting Artistes / Extras | none. **Started:** the suite exists, but 27 cases skip until the artiste and department setup is done |
| Departments | Costumes & Set Sync | 35 sheet bugs. **Started:** the suite exists, but 43 cases skip until a production is created from the Sides script |
| | Transportation | **164 web tests and 84 sheet bugs**: the best-specified of the remaining modules |
| Production | Script & Pages Distribution | 4 sheet bugs |
| | Weather | none |
| Admin | Viewing & Posting Rights Grid | none |
| Ungrouped | Tasks | 4 sheet bugs |

Outside the Tools list: **SOS** (Admin Settings → SOS Receivers) has 21 sheet bugs and no Android suite.

## 2. Automated modules that miss web cases

"Missing" means the feature exists on Android but no Android case covers it. Web-only features
(browser, drag-and-drop, split panes) are left out.

| Module | Web cases | Covered | **Missing** | Wrong (conflict) | Status |
|---|---|---|---|---|---|
| Drive | 326 | 18 | **186** | 1 | Being reworked for the new editor; new-feature cases being added |
| Map | 220 | 70 | **108** | 7 | The Location-overlay flow (10 cases) was added today; the rest are still missing |
| Calendar | 140 | 15 | **105** | 3 | Conflicts fixed today |
| Home | 165 | 30 | **102** | 3 | — |
| Info | 131 | 14 | **101** | 0 | — |
| Confidential Info | 125 | 14 | **93** | 0 | — |
| Catering | 148 | 24 | **85** | 5 | Conflicts fixed today |
| Document Distribution | 101 | 35 | **54** | 1 | DD-24 conflict fixed today |
| C&C | 37 | 16 | **17** | 0 | — |
| Settings | 36 | 22 | **13** | 0 | — |
| Call Sheet (Home unit) | 9 | 4 | **5** | 0 | — |
| Email | 13 | 7 | **5** | 1 | — |
| Schedule Full & One Line | 4 sheet bugs | 0 | **4** | 0 | SCH-01…04 |
| Recce | 4 sheet bugs | 0 | **3** | 0 | RECCE-01, 02, 04 |
| Production Diary / Box | 3 sheet bugs | 0 | **3** | 0 | BOX-01…03 |
| Tools page | 9 | 6 | **2** | 0 | — |
| Tools admin (Customization of Tools) | 2 sheet bugs | 0 | **2** | 0 | TOOLS-01 sorting, TOOLS-02 |
| External Users | 2 sheet bugs | 0 | **2** | 0 | EXT-01, EXT-02 |
| Notifications | 2 sheet bugs | 0 | **2** | 0 | NOTIF-01 (High) |
| Login & Projects | 5 | 4 | 0 | 1 | Kept: Android is right |
| **Total** | | | **~892** | | |

Already matched against the web when they were written today, so they have no gap:
Call Sheet Creation, Production Report Creation, Schedule D.O.D., Crew DOOD, Z-E-Signature.
Sides and Breakup Stills / Continuity have no web cases to compare with.

## 3. Highest-value missing cases first

1. **NOTIF-01 (High):** join and profile-request pushes open the admin approval screen for non-admins.
2. **Document Distribution:** DD-23 "Move here" pushed off screen; DD-11 search keeps hidden files ticked, so Delete removes them; nothing ever actually moves a file.
3. **Calendar:** editing a recurring event (this / following / all); invitees accept and decline.
4. **Home:** a reply quote survives a unit switch; drafts per unit; offline send → retry.
5. **Info / Confidential Info:** the view-only REQUEST bar; rights changed while the chat is open.
6. **Map:** deleting a location type that has locations; zone guards; filter chip counts.
7. **Drive:** Trash restore and empty; rename rules; Tags; share-link expiry and revoke.
8. **Settings:** the per-user View / Post / Download rights grid.
