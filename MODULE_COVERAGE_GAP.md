# Automation coverage — what exists, what's missing

Compared the app source (`~/StudioProjects/Zillit-Android`, branch
`vidya_AI/email_editor_update`) against the 96 test classes in this repo.

**16 areas automated · 31 with no automation at all.**

---

## 1. Already automated

| Area | Suites |
|---|---|
| Home — chat, units, posting rights, media | HomeChat, PostingRights, MediaPreview, MediaSystemPicker, ChatOptions, AllUnitsMedia, RegressionStress |
| Calendar | Calendar |
| Call Sheet | CallSheet, CallSheetUploadMode* |
| Email | Email, EmailDrawer, EmailExtras, EmailActions, EmailCalendar |
| C&C (chat + calling) | 8 Cc* suites |
| Drive | 14 Drive* suites |
| Document Distribution | 17 DocDist* suites |
| Recce | Recce, RecceePagination |
| Catering / Confidential Info / Info | Catering, ConfidentialInfo, Info (all via ToolChatBaseTest) |
| Settings + Admin Settings | 16 suites incl. AdminSettingsGroups* |
| Project create / join / list | CreateProject*, JoinProject*, ProjectList, ProjectFlowE2E |

\* written but never yet completed a green run.

## 2. Gaps inside areas we "cover"

These matter more than new modules, because the suite currently reports these as
passing areas while the gap is invisible.

| Gap | Why it matters |
|---|---|
| **Consent screen** (`consent/ConsentActivity`) | After a fresh install the app shows "Your privacy choices" before Start & Join. `settleLanding()` does not know this screen, so any suite with `requiresLoggedOutStart()` fails with the misleading "No known landing screen appeared". |
| **Email Accounts mailbox tour** | Test cases written (`EMAIL_ACCOUNTS_TOUR_TESTCASES.md`), no automation. |
| **Call Sheet upload mode** | Suite written (17 cases), still never run clean. |
| **SOS** | Only the *admin* "Set/View SOS Receivers" screen is covered. The SOS button and its flow are not. |
| **E-signature** (`docSign`, `forms_and_signature`, `forms_signature`) | No coverage. Easy to think otherwise — a name search matches `EmailSignaturesPage` and `CreateNewDe**signat**ionPage`, neither of which is this. |

## 3. Modules with no automation

Sized by implementation, largest first. Size is a rough proxy for how much there
is to test, not for importance.

### Tier A — start here: reuse what already exists

| Module | Size | Why first |
|---|---|---|
| **Production Report** | 4 files · 5.5k | Chat-style, so `ToolChatBaseTest` applies, **and** it shares the ZL-21154 upload-mode sheet with Call Sheet — `UploadModePage` already exists. Highest reuse of anything on this list. |
| **Asset Register** | 13 files · 2.0k | Small and self-contained; has live/rights/dark-mode behaviour recently changed, so it is also fresh. |
| **Crew List** | 7 files · 0.8k | Small, read-mostly, good first independent suite. |
| **Distribution List** | 6 files · 1.0k | Small; pairs naturally with the Doc Distribution suites already here. |
| **Script Notes** / **Reports** / **Scanner** | ≤5 files each | Tiny. Cheap wins that raise module count without much risk. |

### Tier B — high business value, moderate effort

| Module | Size | Note |
|---|---|---|
| **Purchase Order** | 54 files · 11.0k | Money path: create → approve → commit → reject. Approval states are well-defined, so assertions are clear. |
| **Budget** | 14 files · 8.5k | Chat-style (5 chat files) → `ToolChatBaseTest` applies. Main + department budgets. |
| **Deal Memo** | 50 files · 28.1k | Contracts; recently changed three times in the Nov commits. |
| **Costume & Set Sync** | 81 files · 21.8k | Chat-style entry; large surface (budget, continuity, scene breakdown). |

### Tier C — large, deserve their own plan

| Module | Size | Note |
|---|---|---|
| **Account Hub** | **353 files · 143k** | By far the largest uncovered area — bigger than Drive and Doc Distribution combined. Money-critical, role-gated (accountant vs coordinator). Do not start this as "one suite"; it needs its own breakdown first. |
| **AD Dashboard** | 98 files · 23.2k | |
| **Box Schedule** | 43 files · 15.0k | |
| **Casting** | 14 files · 11.3k | select / shortlist / final / background |
| **Locations / Map** | 45 files · 10.3k | Map interactions are the hardest thing here to automate reliably. |
| **Wardrobe** | 15 files · 9.1k | select / shortlist / final / background |

### Tier D — needs special data, roles or devices

| Module | Size | Blocker |
|---|---|---|
| **SA Portal** | 28 files · 5.0k | Supporting-artiste accounts needed |
| **Sides** | 34 files · 5.0k | depends on script data |
| **Continuity** | 14 files · 4.6k | intra/inter/all variants |
| **Schedule** | 15 files · 3.9k | schedule data per project |
| **Weather** | 25 files · 3.2k | external data; assertions must not depend on real forecasts |
| **Crew DOOD** | 9 files · 3.2k | depends on crew + schedule |
| **Transportation** | `transportationv2` | fleet/driver/trip roles |
| **Personal Project**, **Pre-production**, **Pending User Dashboard** | — | each needs an account in that specific state |

---

## 4. Suggested order

1. **Production Report** — most reuse, proves the Tier-A pattern.
2. **Asset Register → Crew List → Distribution List → Script Notes** — four small suites, fast feedback, module count up quickly.
3. **Purchase Order → Budget** — the money paths, where a regression costs the most.
4. **Deal Memo → Costume & Set Sync** — larger but well-bounded.
5. **Account Hub** — only after the above, and only with its own written plan first.

Run the **Section 2 gaps** alongside Tier A. The consent-screen one in particular
blocks three existing suites today, so it buys back coverage we already paid for.

## 5. Before writing any of it — one check worth doing

**The Tools screen is backend-driven.** Which modules appear depends on the project
and the signed-in user's rights, so "implemented in the app" is not the same as
"visible on your test project". This inventory is from the app source, which tells
us what *can* exist, not what *does* on `ZL TESTING 19 NOV`.

The quickest way to settle it is to open Tools on the test project and read the
list. Any module not on that list cannot be automated there regardless of what
the code contains — and that one check decides which of the tiers above is real
work and which is a project-configuration task first.
