# Email — Accounts "Mailbox Tour" (first-open tutorial)

Test cases for the tutorial an **Accounts department** member sees the first time they
open Email, written from the app source on `vidya_AI/email_tutorial_bugs`.

Source of truth:

| Thing | Where |
|---|---|
| Gate (who sees it, when, how often) | `new_email/ui/tour/MailboxTourGate.kt` |
| The tour itself (5 steps) | `new_email/ui/tour/MailboxTour.kt` |
| Trigger | `new_email/NewEmailLandingPage.kt` → `onMailboxGateChanged()` |
| "Is this an Accounts user" | `new_email/EmailMailboxContext.kt` → `isAccountsUser` |
| Card layout | `res/layout/email_mailbox_tour_card.xml` |
| Department id | `Constants.ACCOUNT_IDENTIFIER = "department_accounts"` |

## How it actually works

**Two different signals, and they are not the same thing.** This is the single most
important fact for testing it:

- **Whether the tour SHOWS** depends on `EmailMailboxContext.isAccountsUser`, which comes
  from the **server**: the project payload's `accounts_mail_box_detail`. A user the server
  does not entitle gets `{}` → blank email → no tour, whatever their profile says.
- **Whether the "already seen" flag is CLEARED** depends on the **profile**:
  `profile.departmentIdentifier != "department_accounts"`.

So a profile change alone does not prove anything until the project payload has been
re-fetched. Any test that changes department must wait for the Email screen to re-evaluate,
not just for the profile screen to save.

**The flag is per user AND per project** — `"${userId}_${projectId}"`. The same user is
treated separately in every project.

**What counts as "seen":**

| Action | Marks seen? | Consequence |
|---|---|---|
| **Finish** on the last step | Yes | never again (in this project, while they stay in Accounts) |
| **X** (`tourClose`) | Yes | never again |
| **Back press** | Yes | never again |
| View destroyed — tab switch, activity recreated | **No** | shows again next time Email is opened |
| Stopped being an Accounts user mid-tour (`cancel()`) | **No** | — |

**The reset rule.** `onProfileChanged()` clears the flag only when a **loaded** profile
positively says "not Accounts". A missing profile, or a blank `departmentIdentifier`, is
deliberately ignored — otherwise an accountant would be shown the tour again on every cold
start, while the profile was still loading.

## Locators

Card ids (on the app package, e.g. `com.zillit.zillitapp.qa:id/tourTitle`):

| Element | id |
|---|---|
| Card | `cardHolder` |
| Close (X) | `tourClose` |
| Step title / body | `tourTitle` / `tourBody` |
| Step dots | `tourDots` |
| Previous / Next (Finish on the last step) | `tourPrev` / `tourNext` |
| Illustration rows | `tourRowPersonal`, `tourRowAccounts`, `tourRowFrom`, `tourArrow` |

The five steps, in order:

1. **Two Mailboxes, One Place** — "As an Accounts department member, you now have a shared Accounts Mailbox…"
2. **Switch Mailboxes Here** (drawer open, highlights the mailbox switcher)
3. **Unread at a Glance** (drawer open)
4. **Send From the Active Mailbox**
5. **Settings Follow the Mailbox**

Buttons: `Previous` / `Next`, and `Finish` on step 5.

## Preconditions

- Two accounts, or one account an admin can move between departments.
- An **Accounts** department must exist in the project, and the project must actually
  entitle the user to an Accounts mailbox — otherwise every case below correctly skips.
- Department is changed from **Settings → Edit My Profile** (the suite's
  `EditMyProfilePage` already has `openDepartmentPicker()` and the
  "If you change department…" confirmation) or by an admin via **User Management**
  (`UserManagementPage.openDepartmentPicker()`).

---

## A. The main scenario

| # | Case | Steps | Expected |
|---|---|---|---|
| A1 | Accounts member sees the tour on first open | Set department to Accounts → open Email | The card appears within a few seconds, step 1 of 5, title "Two Mailboxes, One Place" |
| A2 | It does not come back after Finish | A1 → Next ×4 → **Finish** → leave Email → open Email again | No card |
| A3 | **Change to another department → no tour** | After A2, change department to anything else → open Email | **No card.** `isAccountsUser` is false, so the tour cannot start |
| A4 | **Back to Accounts → tour again** | After A3, change department back to Accounts → open Email | **The card appears again.** Leaving Accounts cleared the seen flag, so this counts as a first time |
| A5 | Non-Accounts user never sees it | A user who has never been in Accounts opens Email | No card, ever |

A3 and A4 are the pair worth running together — A4 only passes because A3 happened, and
that dependency is the actual feature.

## B. Navigating the tour

| # | Case | Expected |
|---|---|---|
| B1 | Five steps, in order | Titles 1→5 as listed above; `tourDots` shows five |
| B2 | Next advances | Step 1 → 2 → … → 5 |
| B3 | Previous goes back | From step 3, Previous returns to step 2 with its own title/body |
| B4 | Previous is hidden on step 1 | `tourPrev` not displayed at index 0 |
| B5 | Last button says Finish | On step 5 `tourNext` reads "Finish", not "Next" |
| B6 | Steps 2 and 3 open the drawer | The folder drawer is open and the mailbox switcher is highlighted |
| B7 | The drawer closes at the end | After Finish, the drawer is no longer open |

## C. Dismissing — what counts as seen

| # | Case | Steps | Expected |
|---|---|---|---|
| C1 | X marks it seen | Open tour → tap `tourClose` → reopen Email | No card |
| C2 | Back marks it seen | Open tour → press Back → reopen Email | No card; Back closes the tour, it does **not** leave Email |
| C3 | Finish marks it seen | Step 5 → Finish → reopen Email | No card |
| C4 | **Switching tabs does NOT mark it seen** | Open tour → switch to Home → back to Email | **The card appears again.** Intended: the view was destroyed, not dismissed |
| C5 | Seen survives a relaunch | Finish the tour → kill and reopen the app → open Email | No card |

C4 reads like a bug when you meet it by hand. It is in `MailboxTour.kt` on purpose —
`onDestroy → end(markSeen = false)` — so it is recorded here as expected behaviour. If QA
decides a tab switch should count, that is a product decision, not a test fix.

## D. Scope

| # | Case | Expected |
|---|---|---|
| D1 | Per project | Finish the tour in project A; open Email in project B where the user is also Accounts → the card appears |
| D2 | Per user | User X finishes it; user Y in the same project and department still sees it |
| D3 | No Accounts mailbox → no tour | In a project where the server sends an empty `accounts_mail_box_detail`, an Accounts-department user sees **no** card |

D3 is the case that separates the two signals. Worth running even though it looks
redundant with A5 — it fails differently if someone ever moves the gate onto the profile
department alone.

## E. Timing and interruptions

| # | Case | Expected |
|---|---|---|
| E1 | Appears after the list lays out | A ~400 ms delay then the card; it must not flash before the drawer and list are measured |
| E2 | Losing Accounts mid-tour cancels it | While the tour is open, have an admin move the user out of Accounts → the card disappears and is **not** marked seen |
| E3 | Not started twice | Re-open the drawer repeatedly while the tour is up → still exactly one card |
| E4 | Rotation | Card survives or re-appears cleanly; no duplicate cards |

---

## Notes for automating this

1. **`assumeTrue`, not `assertTrue`, for the entitlement.** Whether the project grants an
   Accounts mailbox is environment, not behaviour. Without one, every case here should
   report SKIPPED with "project has no Accounts mailbox", never a red failure.
2. **Changing department is a real, slow, two-sided change.** The profile save shows an
   "If you change department…" confirmation (already handled by
   `EditMyProfilePage.isDepartmentWarningShown()`), and the tour only re-evaluates after the
   project payload refreshes. Wait for the Email screen to settle, not for the profile
   screen to close.
3. **A3 → A4 must run in order** and in one session, as a ladder suite
   (`relaunchBetweenTests() → false`), because A4's expectation depends on A3 having
   cleared the flag.
4. **Put the department back when finished.** These cases move a real user between real
   departments on a shared project; leaving them in Accounts changes what every later suite
   sees.
5. **Two risks worth raising before writing the code:**
   - The reset depends on the profile flow emitting a **non-blank** `departmentIdentifier`.
     If the profile response ever omits that field after a department change, the flag is
     never cleared and A4 silently fails — the tour simply never returns. Confirm the field
     is present in the response before trusting A4.
   - Nothing marks the tour seen when it is cancelled because the user stopped being an
     accountant (E2). That is correct, but it means a user bounced in and out of Accounts
     mid-tour will see it from step 1 each time.
