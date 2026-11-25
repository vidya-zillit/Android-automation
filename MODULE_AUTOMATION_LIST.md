# Module Automation List — ZL-Android-Automation

Built from the **live Tools screen** on `emulator-5554`, project `ZL-Android-Automation`,
app `com.zillit.zillitapp.dev` **Zillit_32537-dev**, source branch `vidya_AI/bug_fix_nov` @ `bd8b8fc7`.

Captured 2026-11-19 by walking the Tools list and reading `groupTitleTv` / `groupCountTv` / `nameTv`.
These are the tools **actually enabled on this project** — not everything that exists in source.

Coverage column last updated 2026-11-23 from the suites under `src/test/java/com/zillit/automation/tests/<module>/`.

**37 tools in 7 groups. 17 covered, 20 not covered.** (Supporting Artistes and Costumes & Set Sync in progress 2026-11-23.)

---

## ACCOUNTS / PAYROLL (11) — 0 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 1 | Asset Register | `AssetRegisterActivity` | — |
| 2 | Budget Builder | `BudgetChatActivity` | — |
| 3 | Cost Report | `CostReportActivity` | — |
| 4 | Deal Memo | `DealMemoLandingActivity` | — |
| 5 | Message Accounts | `AccountsActivity` | — |
| 6 | Payroll | `TimecardPayrollLandingActivity` | — |
| 7 | Petty Cash Expenses | `CashExpenseLandingActivity` | — |
| 8 | Production Expense Cards | `CardExpenseLandingActivity` | — |
| 9 | Purchase Order | `POLandingActivity` | — |
| 10 | Talent Deals | Account Hub → Deal Memo (`ahdm_tal_*`) | — |
| 11 | Time Card | `TimecardHubActivity` | — |

## ADS (8) — 5 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 12 | AD Dashboard | `AdDashboardActivity` (+27 more) | — |
| 13 | **Call Sheet Creation** | `ProjectDetailActivity` (tile `callsheet_tool`) | ✅ `CallSheetCreationTest`, `CallSheetApprovalTest` (2 devices) (`--call-sheet-creation`) — [CALL_SHEET_CREATION_TESTCASES.md](CALL_SHEET_CREATION_TESTCASES.md) |
| 14 | **Catering** | `CateringActivity` | ✅ `CateringTest` (via `ToolChatBaseTest`) |
| 15 | Create Shooting Order | `new_boxschedule` / `schedule` | — |
| 16 | **Production Report Creation** | `ProductionReportDashboardActivity` | ✅ `ProductionReportTest`, `ProductionReportApprovalTest` (2 devices) (`--production-report`) — [PRODUCTION_REPORT_CREATION_TESTCASES.md](PRODUCTION_REPORT_CREATION_TESTCASES.md) |
| 17 | **Schedule D.O.D.** | `DodActivity` | ✅ `ScheduleDodTest` (`--schedule-dod`) — [SCHEDULE_DOD_TESTCASES.md](SCHEDULE_DOD_TESTCASES.md) |
| 18 | **Schedule Full & One Line** | `ScheduleDistActivity` | ✅ `ScheduleDistTest` (`--schedule-dist`) — [SCHEDULE_DIST_MODULE_TESTCASES.md](SCHEDULE_DIST_MODULE_TESTCASES.md) |
| 19 | Supporting Artistes / Extras | `SaPortalActivity` | — |

## DEPARTMENTS (3) — 1 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 20 | **Breakup Stills** | `ContinuityPage` *(see note)* | ✅ `ContinuityTest` (`--continuity`) — [CONTINUITY_MODULE_TESTCASES.md](CONTINUITY_MODULE_TESTCASES.md) |
| 21 | Costumes & Set Sync | `CssActivity` | — |
| 22 | Transportation | `TransportationActivity` | — |

> **Note on row 20 — read before automating Continuity.** `FileCabinetContinuityActivity`
> is NOT the Tools route. Its only launcher is commented out
> (`settings/admin/views/FileCabinetDocumentMainPage.kt:112-121`) and it belongs to the
> admin File Cabinet screen, not to a Tools tile. The live route from Tools is
> `CONTINUITY_TOOL -> ContinuityPage` (`bottomNav/tools/Tools.kt:558`), with
> `ContinuitySceneListPage` behind it. Captured 2026-11-21 while planning the Continuity
> suite; the original row was read off the class name and sent that work to a dead screen.

## LOCATION (2) — 2 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 23 | **Map** | `MapViewActivity` / `ShootingLocationActivity` | ✅ 12 `Map*` suites + `LocationToolTest` (`--map-all`) — 16 location-overlay cases skip by design while the Location tool is off |
| 24 | **Recce** | `RecceActivity` | ✅ `RecceTest`, `RecceePaginationTest` |

## PRODUCTION (7) — 5 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 25 | **Crew DOOD** | `CrewDoodActivity` | ✅ `CrewDoodTest` (`--crew-dood`) — [CREW_DOOD_TESTCASES.md](CREW_DOOD_TESTCASES.md) |
| 26 | **Document Distribution** | `DocumentDistributionActivity` | ✅ 18 `DocDist*` suites |
| 27 | **Production Diary/Box Schedule** | `BoxScheduleActivity` | ✅ 11 `Box*` suites (`--box`) — [BOX_MODULE_TESTCASES.md](BOX_MODULE_TESTCASES.md) |
| 28 | Script & Pages Distribution | `ScriptDistActivity` | — |
| 29 | **Sides** | `SidesMainActivity` | ✅ `SidesTest` (`--sides`) — [SIDES_MODULE_TESTCASES.md](SIDES_MODULE_TESTCASES.md) |
| 30 | Weather | `WeatherProActivity` | — |
| 31 | **Z-E-Signature** | `DocSignMainActivity` | ✅ `ESignatureTest` (`--e-signature`) — [E_SIGNATURE_TESTCASES.md](E_SIGNATURE_TESTCASES.md) |

## ADMIN (5) — 4 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 32 | **Confidential Info** | `confidentialInfo` | ✅ `ConfidentialInfoTest` |
| 33 | **External Users** | `external_invitees` flow | ✅ `ExternalUserTest`, `ExternalUserDropdownProbeTest` (`--external-users`) |
| 34 | **Info** | Bulletin chat | ✅ `InfoTest` |
| 35 | Viewing & Posting Rights Grid | `PermissionGridMainActivity` | — (`PostingRightsTest` is Home-unit gating, a different screen) |
| 36 | **Z-Drive** | `DriverMainActivity` / `ZillitDriveActivity` | ✅ 15 `Drive*` suites — cases & status in `DRIVE_MODULE_TESTCASES.md` |

## UNGROUPED (1) — 0 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 37 | Tasks | — | — |

---

## Still pending (20)

- **Accounts / Payroll (11):** Asset Register, Budget Builder, Cost Report, Deal Memo, Message
  Accounts, Payroll, Petty Cash Expenses, Production Expense Cards, Purchase Order, Talent Deals, Time Card
- **ADs (3):** AD Dashboard, Create Shooting Order, Supporting Artistes / Extras (in progress)
- **Departments (2):** Costumes & Set Sync (in progress), Transportation
- **Production (2):** Script & Pages Distribution, Weather
- **Admin (1):** Viewing & Posting Rights Grid
- **Ungrouped (1):** Tasks

---

## Suggested order

**Tier 1 — bulletin-chat tools.** These reuse the same `rvChat` + `etMessage` + `ivAttachment`
layout that `ToolChatBaseTest` already drives, so each is mostly a thin page object plus a
subclass. Cheapest coverage per module by a wide margin.

| Tool | Layout | Shares pattern with |
|------|--------|--------------------|
| Message Accounts | `activity_accounts.xml` | `activity_catering.xml` (covered) |
| Budget Builder | `activity_budget_chat_page.xml` | same |
| Production Report (chat) | `fragment_production_report_chat.xml` | same |

**Tier 2 — self-contained list/detail tools.** One page object, one suite, no shared state.
Crew DOOD · Schedule D.O.D. · Costumes & Set Sync · Transportation · Weather

**Tier 3 — document pipelines.** Structurally like the DocDist suites, so the patterns transfer.
Script & Pages Distribution · Z-E-Signature · Asset Register

**Tier 4 — large subsystems.** Each needs its own multi-suite plan.
AD Dashboard (28 activities) · Account Hub family — Cost Report, Petty Cash, Expense Cards,
Payroll, Time Card, Talent Deals (81 activity files) · Purchase Order · Deal Memo ·
Production Report Creation · Create Shooting Order · SA Portal · Permission Grid · Tasks

---

## Blockers

- **Consent screen is unhandled.** First launch lands on `ConsentActivity`
  ("Your privacy choices", Accept all / Reject all). No suite references it; a fresh install or
  cleared app data will strand every suite before `settleLanding()`. Needs a `BaseTest` handler.
- **Email accountant tutorial has no source here.** `MailboxTour.kt` is absent from
  `vidya_AI/bug_fix_nov`; it exists only on `vidya_AI/email_tutorial_bugs`. Those cases stay unrunnable.
- **Branch is behind.** `vidya_AI/bug_fix_nov` tip is 12 Nov; `development` is 17 Nov.
