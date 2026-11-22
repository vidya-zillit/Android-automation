# Sides — test cases and findings

**Sides** are the day's pages: a PDF cut out of the script down to the scenes being shot,
handed to the unit on the call sheet's day. Tools → **Sides** opens
`bottomNav/tools/sides/ui/SidesMainActivity`, which hosts two tabs of its own
(`sides_bottom_nav`: `nav_sides`, `nav_script`) over a Script-Distribution backend at
`/api/v2/`.

Automation: [`SidesTest`](src/test/java/com/zillit/automation/tests/sides/SidesTest.java),
30 cases, `TestOrder.SIDES`. One run:

```bash
./run-ui.sh --sides
```

**How to read this sheet.** Every case says what a user does and what must then be true.
Quoted wording is the app's own, because the automation matches on it; `id` in backticks is
a real resource id, read from the layout.

---

## The shape of the data

```
Script                       a title, and OPTIONALLY a file (PDF / .fdx)
  ├── versions               each upload makes one — v1, v2 … with its own page count
  │     └── scenes           parsed out of the PDF's text by the backend
  └── pages                  hand-added scene pages (scene number, colour, description)

Side                         scenes picked from one version, rendered to a PDF
  ├── status                 generating → Ready (or failed)
  └── History                archived sides, lazily loaded under the active list
```

Two gates govern the action buttons (spec §9.1, `SidesViewModel`):

| Button | Gate | Toast when it is shut |
|---|---|---|
| Generate Sides | `hasAnyScript` — any script at all | "No active script or pages found. Please upload script to pages to generate sides" |
| Autogenerate Sides | `hasPublishedScript` — `scripts.any { it.hasFile }` | "No published script available. Please upload script to generate sides" |

So a script saved with **no** file opens Generate but not Autogenerate. That is the one
difference the two gates have, and SI-20 / SI-21 are the pair that proves it.

---

## 1. Module shell · `SI-0`

| # | Case | Expected |
|---|---|---|
| SI-01 | Tools → Sides | Header "SIDES"; `btnGenerate` and `btnAutogenerate` both offered; the History section is on the screen |
| SI-02 | Open the Script tab | `nav_script` → `fragment_scripts` loads and the toolbar header follows to "SCRIPT" |
| SI-03 | Back to the Sides tab | Header returns to "SIDES" and both action buttons come back |

## 2. Scripts and their files · `SI-1`

| # | Case | Expected |
|---|---|---|
| SI-10 | Add Script | Dialog asks for a name and offers a file row — the file is OPTIONAL, which is what makes a name-only script possible |
| SI-11 | Save with a blank name | Refused, dialog stays open. `AddScriptDialog.submit()` toasts and returns on an empty name |
| SI-12 | Save with a name only | The card appears and offers "Upload Script" / delete — **not** Replace, View or Download |
| SI-13 | Upload a file onto it | The same card now reads "Replace" and offers View and Download |
| SI-14 | Page count matches the file | The card's meta names 5 pages for the 5-page fixture. The count comes from the backend parsing the PDF *after* accepting the upload, so the first card back reads "1 pages" and corrects itself — the case waits for it to settle |
| SI-15 | Replace the file | A new version: the meta changes, and the page count is right on this version too. If it were right here and wrong on SI-14, the parse would only be running on replaces |
| SI-16 | A list taller than the screen | Builds three scripts — which do not fit on one screen — and reads all three back, then taps an action on the last one. Guards the scrolling readers below: without this case, nothing in the suite would ever make a list long enough to exercise them |

## 3. The two gates · `SI-2`

| # | Case | Expected |
|---|---|---|
| SI-20 | Generate with any script | `GenerateSidesActivity` opens |
| SI-21 | Autogenerate with a script that has a file | The bottom sheet opens and names that script as the active one |

## 4. The Generate form · `SI-3`

| # | Case | Expected |
|---|---|---|
| SI-30 | The form's shape | The selected script shows; Rearrange is offered on BOTH display modes; Cross-out is the default |
| SI-31 | Submit with nothing picked | Refused — "Select at least one scene or page". A side with no scenes is a blank hand-out |
| SI-32 | Rearrange | Reveals the order input, and hides it again |
| SI-33 | Expand a version | Lists the scenes the backend found in that PDF. The fixture carries five numbered scene headings precisely so an empty picker means something |
| SI-34 | Pick two scenes | The summary counts 2 |
| SI-35 | Generate → review → Publish | Reaches the ready stage; the review offers View, Download and Publish; "Move to Document Distribution" is **not** reachable (see finding 4); Publish returns to the list with the side on it |

The review is a **stage of the Generate activity**, not a screen of its own: it cannot be
navigated back to, and the activity finishes itself on Publish. Generate, review and publish
therefore belong in one case — split across three, the later two skipped for want of a
review the first had already consumed.

## 5. The list row · `SI-4`

| # | Case | Expected |
|---|---|---|
| SI-40 | The row's shape | Title, status and meta line all present |
| SI-41 | Scene list vs scene count | They must agree (see finding 1) |
| SI-42 | The row overflow | All four actions on a side with a PDF: View · Download · Send via Document Distribution · Delete |
| SI-43 | View | Opens `SidesWebViewActivity`, the in-app PDF viewer — not a browser |
| SI-44 | Delete asks first | The confirm appears, and Cancel keeps the side. A side is the unit's hand-out; a mis-tap must not take it |
| SI-45 | Expand History | Shows its rows, or says there are none (see finding 2) |

## 6. The Autogenerate sheet · `SI-5`

| # | Case | Expected |
|---|---|---|
| SI-50 | Submit with no call sheet | Refused — "Select or upload a call sheet first". The call sheet is what tells it which scenes the day covers. *Skips on a project that already has call sheets* |
| SI-51 | Upload a call sheet | It is then offered for selection, by name |
| SI-52 | Section headings | Each heading sits above the controls it heads (see finding 3) |
| SI-53 | Empty call-sheet list | Says it is empty. *Skips on a project that already has call sheets* |
| SI-54 | Empty schedule list | Says it is empty (see finding 2) |

## 7. Cleanup · `SI-6`

| # | Case | Expected |
|---|---|---|
| SI-60 | Sweep | No side or script whose name starts `ZL_AUTO` is left on the project — including ones a run that broke halfway left behind, and ones sitting below the fold |

---

## Findings

Four cases fail on every run, on the same evidence. They are defects, not flakes.

**1. A row's scene list and its scene count contradict each other — `SI-41`**
The row reads `Scenes: 1, 2, 3  ·  1 scene(s)  ·  Nov 20, 2026`. The adapter prints
`sceneNumbers` and `totalScenes` as two independent facts straight from the backend and
never reconciles them, so the user is told two different things in one line.

**2. Empty lists say nothing — `SI-45`, `SI-54`**
`rvHistory` is set VISIBLE when History is expanded, but an empty RecyclerView has no
children and therefore no height: the chevron turns and the screen does not change, so an
empty section is indistinguishable from a failed load. The Autogenerate sheet's schedule
list has the same gap. (The scripts list, by contrast, does have `layEmpty` with
"No scripts yet" — so the pattern exists in the module already.)

**3. The SCHEDULE heading renders below its own action — `SI-52`**
Measured: the heading is at y=1476 and its "+ Upload new" at y=1438. The action is drawn
above the heading it belongs to, so it reads as the action of the section above it.

**4. "Move to Document Distribution" is implemented but unreachable**
`btnMove` is in `view_sides_review.xml`, both hosts wire a click listener to it, and the
chain behind it is complete down to the Document Distribution call — but it is declared
`android:visibility="gone"` and nothing ever makes it visible. `SI-35` asserts the current
behaviour deliberately: if that assertion starts failing, the path has been switched on and
the case should be inverted.

Two more, noted while exploring and not worth a case of their own:

- No "no scenes detected" message. `noScenesDetected` exists in the ViewModel;
  `item_sides_scene_picker.xml` has no view to show it, so a version whose PDF yields no
  scenes expands to nothing. `SI-33` catches the symptom.
- The call-sheet and schedule lists in the sheet have no empty state either (finding 2
  covers the schedule one; the call-sheet case skips on this project because it already has
  call sheets).

---

## Two lessons the readers in this module are built on

**Appium never looks upward from an element.** An element-relative XPath is scoped to that
element's own subtree, so `./ancestor::*[…]` from a row's title matches nothing — it is not
that the ancestor is missing, it is that the search never leaves the subtree. Every reader
here that once walked up from a title returned an empty list while the rows were plainly on
screen.

**And there is often nothing to walk up to.** The LinearLayouts a row is assembled from are
routinely pruned out of the accessibility tree — the script card's `lay_actions` simply is
not there. What survives is position, so rows are sliced by geometry:
`BasePage.bandsOf(anchorId)` gives one vertical band per anchor, running from the anchor's
own top less its height (so a sibling on the anchor's line is included) up to the same point
for the next anchor. No density constants, nothing to tune per screen.

**A row below the fold is not in the tree either, so reading the window is not reading the
list.** This is the same absence wearing a different hat, and it is the one that bites
later rather than immediately: everything passes on a project with three sides and starts
lying on a project with thirty. `BasePage.readWholeList(containerId, anchorId, …)` reads the
window, and if the list visibly **ends** on screen — blank space after the last row — that
was the whole list and nothing is scrolled, so short lists cost no gestures. Otherwise it
follows the container, merging windows on a key, and puts the scroll position back.
Acting on a row needs the same treatment, since a row that is not rendered cannot be
tapped: `scrollToRow` runs before every per-row tap. Three readers needed it beyond the
obvious lists — `summaryCount()` (the count label slides below the chips, exactly as it did
in Box), the History section (it sits *under* the sides list, so a screenful of sides hides
it completely) and the "does this screen say it is empty" checks, because "it says nothing"
is only worth reporting once the whole screen has been looked at.

## Fixtures

The runner pushes both to `/sdcard/Documents` before the run:

| File | What it is |
|---|---|
| `zl_sides_script.pdf` | 5 pages, one numbered scene heading on each ("1   INT. PRODUCTION OFFICE - DAY" …). The backend derives the scene list from the PDF's **text**, so a blank stub leaves every picker empty with nothing on screen to say why |
| `zl_sides_callsheet.pdf` | A one-page call sheet for the Autogenerate flow |

Everything the suite creates is named `ZL_AUTO_…` and `SI-60` sweeps all of it.
