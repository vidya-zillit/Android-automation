# Module Automation List — ZL-Android-Automation

Built from the **live Tools screen** on `emulator-5554`, project `ZL-Android-Automation`,
app `com.zillit.zillitapp.dev` **Zillit_32537-dev**, source branch `vidya_AI/bug_fix_nov` @ `bd8b8fc7`.

Captured 2026-11-19 by walking the Tools list and reading `groupTitleTv` / `groupCountTv` / `nameTv`.
These are the tools **actually enabled on this project** — not everything that exists in source.

**37 tools in 7 groups. 6 covered, 31 not covered.**

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

## ADS (8) — 2 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 12 | AD Dashboard | `AdDashboardActivity` (+27 more) | — |
| 13 | **Call Sheet Creation** | `CallSheetDetailActivity` / `CallSheetEditorActivity` | ✅ `CallSheetTest`, `CallSheetUploadModeTest` |
| 14 | **Catering** | `CateringActivity` | ✅ `CateringTest` (via `ToolChatBaseTest`) |
| 15 | Create Shooting Order | `new_boxschedule` / `schedule` | — |
| 16 | Production Report Creation | `ProductionReportDashboardActivity` | — |
| 17 | Schedule D.O.D. | `DodActivity` | — |
| 18 | Schedule Full & One Line | `ScheduleDistActivity` | — |
| 19 | Supporting Artistes / Extras | `SaPortalActivity` | — |

## DEPARTMENTS (3) — 0 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 20 | Breakup Stills | `FileCabinetContinuityActivity` | — |
| 21 | Costumes & Set Sync | `CssActivity` | — |
| 22 | Transportation | `TransportationActivity` | — |

## LOCATION (2) — 1 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 23 | Map | `MapViewActivity` / `ShootingLocationActivity` | — |
| 24 | **Recce** | `RecceActivity` | ✅ `RecceTest`, `RecceePaginationTest` |

## PRODUCTION (7) — 1 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 25 | Crew DOOD | `CrewDoodActivity` | — |
| 26 | **Document Distribution** | `DocumentDistributionActivity` | ✅ 18 `DocDist*` suites |
| 27 | Production Diary/Box Schedule | `BoxScheduleActivity` | — |
| 28 | Script & Pages Distribution | `ScriptDistActivity` | — |
| 29 | Sides | `SidesMainActivity` | — |
| 30 | Weather | `WeatherProActivity` | — |
| 31 | Z-E-Signature | `DocSignMainActivity` | — |

## ADMIN (5) — 3 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 32 | **Confidential Info** | `confidentialInfo` | ✅ `ConfidentialInfoTest` |
| 33 | External Users | `external_invitees` flow | — (`InviteUsersTest` is Settings → Invite Users, a different screen) |
| 34 | **Info** | Bulletin chat | ✅ `InfoTest` |
| 35 | Viewing & Posting Rights Grid | `PermissionGridMainActivity` | — (`PostingRightsTest` is Home-unit gating, a different screen) |
| 36 | **Z-Drive** | `DriverMainActivity` / `ZillitDriveActivity` | ✅ 15 `Drive*` suites |

## UNGROUPED (1) — 0 covered

| # | Tool | Activity | Automation |
|---|------|----------|-----------|
| 37 | Tasks | — | — |

---

## Suggested order

**Tier 1 — bulletin-chat tools.** These reuse the same `rvChat` + `etMessage` + `ivAttachment`
layout that `ToolChatBaseTest` already drives, so each is mostly a thin page object plus a
subclass. Cheapest coverage per module by a wide margin.

| Tool | Layout | Shares pattern with |
|------|--------|--------------------|
| Message Accounts | `activity_accounts.xml` | `activity_catering.xml` (covered) |
| Budget Builder | `activity_budget_chat_page.xml` | same |
| Breakup Stills | `activity_file_cabinet_chat_page.xml` | same |
| Production Report (chat) | `fragment_production_report_chat.xml` | same |

**Tier 2 — self-contained list/detail tools.** One page object, one suite, no shared state.
Crew DOOD · Schedule D.O.D. · Costumes & Set Sync · Transportation · Weather · Sides

**Tier 3 — document pipelines.** Structurally like the DocDist suites, so the patterns transfer.
Script & Pages Distribution · Schedule Full & One Line · Z-E-Signature · Asset Register

**Tier 4 — large subsystems.** Each needs its own multi-suite plan.
AD Dashboard (28 activities) · Account Hub family — Cost Report, Petty Cash, Expense Cards,
Payroll, Time Card, Talent Deals (81 activity files) · Purchase Order · Deal Memo ·
Production Report Creation · Box Schedule · Map · SA Portal · Permission Grid

---

## Blockers

- **Consent screen is unhandled.** First launch lands on `ConsentActivity`
  ("Your privacy choices", Accept all / Reject all). No suite references it; a fresh install or
  cleared app data will strand every suite before `settleLanding()`. Needs a `BaseTest` handler.
- **Email accountant tutorial has no source here.** `MailboxTour.kt` is absent from
  `vidya_AI/bug_fix_nov`; it exists only on `vidya_AI/email_tutorial_bugs`. Those cases stay unrunnable.
- **Branch is behind.** `vidya_AI/bug_fix_nov` tip is 12 Nov; `development` is 17 Nov.
