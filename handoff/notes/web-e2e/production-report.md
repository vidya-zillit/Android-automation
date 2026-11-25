# Web E2E — Production Report

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: moderate — Manage tabs and approvals

## Web test cases (13)

### Production Report

- **PR-S01: module shell renders with the manage tabs @smoke** — passed (productionreport.spec.js:162)
  - Where: Tools → Production Report · account: iPhone XRR
  - Steps: Open Tools → Production Report and wait for the “Production Report Creation” title → Check that the Approvals tab is shown → Check whether the Drafts and Published tabs are shown → Check that Drafts and Published are either both shown or both hidden
  - Expected: Production Report opens with its Approvals tab, and the Drafts and Published tabs appear or hide together (both depend on posting rights).

- **PR-S02: tab switching keeps the shell mounted @smoke** — passed (productionreport.spec.js:180)
  - Where: Tools → Production Report · account: iPhone XRR
  - Steps: Open the browser’s developer console to watch for page errors → Open Tools → Production Report and wait for the “Production Report Creation” title → Click the Approvals tab → Check that the Sent sub-tab appears → Click the Drafts tab → Check that the Production Report screen is still shown → Check that no page error was reported while switching tabs
  - Expected: Switching between Approvals and Drafts keeps the Production Report screen open without any page error.

- **PR-S03: Approvals Sent/Received sub-tabs swap the list @smoke** — passed (productionreport.spec.js:196)
  - Where: Tools → Production Report → Approvals tab · account: iPhone XRR
  - Steps: Open Tools → Production Report and wait for the “Production Report Creation” title → Click the Approvals tab → Check that its sub-tabs include Sent, Received or Finalized → Click the second sub-tab → Check that it becomes the selected sub-tab and the first is no longer selected → Click the first sub-tab again → Check that it is selected again
  - Expected: The Approvals sub-tabs (Sent / Received / Finalized) switch the selected list when clicked.

- **PR-P03/PR-P05: Published tab renders the hero card and history @smoke** — passed (productionreport.spec.js:220)
  - Where: Tools → Production Report → Published tab · account: iPhone XRR
  - Steps: Open Tools → Production Report and wait for the “Production Report Creation” title → Click the Published tab → Check that either “Currently Published” or “No published production reports” is shown → If a report is published, check that the card shows Shoot Day, Published At and a View PDF button → If a “History (n)” button is shown, click it → Check that it turns into “Hide History”
  - Expected: The Published tab shows the currently published report card (Shoot Day, Published At, View PDF) and its History opens, or the empty message when nothing is published.

- **PR-C01/PR-C02/PR-C03: template picker lists three layouts and cancels clean** — notrun (productionreport.spec.js:248)
  - Where: Tools → Production Report → Create Template · account: iPhone XRR
  - Steps: Open Tools → Production Report and wait for the “Production Report Creation” title → Click Create Template → Check that the template picker shows Template 1 and Template 3, with “Template 1 preview” shown → Click Template 2 → Check that “Template 2 preview” and a “Use Template 2” button are shown → Click Cancel → Check that the Production Report Editor did not open and Create Template is shown again
  - Expected: The template picker lists the template layouts, switches the preview when another is chosen, and Cancel closes it without opening the editor.

- **PR-D01/PR-D02/PR-D05: create a draft, see it listed, delete it** — notrun (productionreport.spec.js:279)
  - Where: Tools → Production Report → Create Template · account: iPhone XRR
  - Steps: Open Tools → Production Report and wait for the “Production Report Creation” title → Click Create Template; if the template picker opens, click “Use Template 1” → Wait for the Production Report Editor and its A4 preview page → Click Save As, type the draft name “[e2e-pr] draft <time>” in “Enter draft name” and click Confirm → If a “No Published Call Sheet” notice appears, click OK → Check that the editor closes and the Drafts list shows the new draft with status Draft → Open the draft’s Actions menu and choose Delete → Click Delete in the “Delete Production Report” dialog → Check that the draft is gone from the list
  - Expected: A new report saved with Save As appears in Drafts with status Draft, and deleting it removes it from the list.

- **PR-D03/PR-E01: edit a draft, change a cell, save a revision** — notrun (productionreport.spec.js:304)
  - Where: Tools → Production Report → Drafts tab · account: iPhone XRR
  - Setup: Creates a draft “[e2e-pr] edit <time>” from Create Template with Save As.
  - Steps: Open Tools → Production Report and wait for the “Production Report Creation” title → Click Create Template; if the template picker opens, click “Use Template 1” → Wait for the Production Report Editor and its A4 preview page → Click Save As, type the draft name “[e2e-pr] edit <time>” in “Enter draft name” and click Confirm → If a “No Published Call Sheet” notice appears, click OK → Open the draft’s Actions menu and choose Edit → In the preview, click the first “Click to edit” cell and check that an edit field appears → Click the Save button, then Save in its menu → Check that the editor closes and the Drafts list still has exactly one row with that name
  - Expected: An existing draft opens in the editor, a preview cell opens its edit field, and Save writes a new revision without creating a second row. (Despite the title, the test does not type a change into the cell.)

- **PR-A01: Send → For Signature with no approvers is refused** — notrun (productionreport.spec.js:348)
  - Where: Tools → Production Report → Drafts tab · account: iPhone XRR
  - Setup: Creates a draft “[e2e-pr] noapprovers <time>” from Create Template with Save As.
  - Steps: Open Tools → Production Report and wait for the “Production Report Creation” title → Click Create Template; if the template picker opens, click “Use Template 1” → Wait for the Production Report Editor and its A4 preview page → Click Save As, type the draft name “[e2e-pr] noapprovers <time>” in “Enter draft name” and click Confirm → If a “No Published Call Sheet” notice appears, click OK → Open the draft’s Actions menu and choose Send for Approval → Check that “Choose how to send this production report” is shown → Click For Signature → Check that the app refuses with “No approvers exist for this report”
  - Expected: Sending a report for signature when it has no approvers is refused with a “No approvers…” message, so nothing is sent. If the project supplies default approvers, the test fails instead.

### Production Report — blocked

- **PR-A02/A03: send for comments / signature — notifies real users; needs a sandbox project + second account** — notrun (productionreport.spec.js:397)
  - Where: Tools → Production Report → Drafts tab · account: iPhone XRR and Lava Device
  - Setup: A draft “[e2e-pr] …” with Lava Device as recipient or approver.
  - Steps: As iPhone XRR, open the draft’s Send for Approval → Choose For Comments, pick Lava Device in the recipient list and click Send → Check that the row leaves Drafts and shows in Approvals → Sent as For Comments → Alternatively choose For Signature on a draft that has approvers → Check that the row shows Pending Signature in Approvals → Sent
  - Expected: Sending for comments or for signature moves the report from Drafts to Approvals → Sent with the matching status.

- **PR-A04/A05/A06: approve / reject — the sender cannot approve their own report; needs storageStateB** — notrun (productionreport.spec.js:398)
  - Where: Tools → Production Report → Approvals tab → Received · account: iPhone XRR and Lava Device
  - Setup: A report sent by iPhone XRR with Lava Device as recipient or approver.
  - Steps: As Lava Device, open Approvals → Received → Approve the report (internal stage) and confirm, or approve the final stage with or without a signature → Or reject it with a reason → As iPhone XRR, check the row’s status (Final Approved or Final Rejected)
  - Expected: The second user can approve or reject a report they received, and the sender sees the resulting status.

- **PR-P02: publish — writes the PDF into the project unit chat everyone sees; needs sandbox agreement** — notrun (productionreport.spec.js:399)
  - Where: Tools → Production Report → Approvals tab → Finalized · account: iPhone XRR
  - Setup: A report in Final Approved state.
  - Steps: Open Approvals → Finalized and click Publish on the report → Choose New and confirm → Open the Published tab → Check that the card reads “Currently Published”
  - Expected: Publishing as New moves the report to Published as the current report.

- **PR-P06: attach document — same blast radius as publish** — notrun (productionreport.spec.js:400)
  - Where: Tools → Production Report → Published tab · account: iPhone XRR
  - Setup: A published report.
  - Steps: Open the Published tab → Click Attach Document and pick a document → Confirm in the document dialog → Check that the document is posted into the production report chat
  - Expected: Attach Document posts the chosen document into the project’s production report chat.

- **PR-P07: publish to Doc Distribution — cross-module write into the DD library** — notrun (productionreport.spec.js:401)
  - Where: Tools → Production Report → Published tab · account: iPhone XRR
  - Setup: A published report.
  - Steps: Open the Published tab → Click Send to Document Distribution → Open Tools → Document Distribution → Check that the report’s PDF is in the library
  - Expected: Send to Document Distribution uploads the report’s PDF into the Document Distribution library.

## Sheet bugs (6)

- **PR-01** The Name and Save As sheets have the wrong title — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Create a report and tap Save (name prompt). 2. Open a draft and tap Save As.
  - Expected: "Name Your Draft" and "Save As New Draft".
  - Actual: The sheets showed the wrong prompt's title.

- **PR-02** Report chat search count doesn't match the highlighted messages — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Send a message containing "PR-016" in a report chat. 2. On the other device, search for "PR-016".
  - Expected: "1 Match Found" with exactly one highlight.
  - Actual: The count and the highlights used different matching rules and disagreed.

- **PR-03** A rejected report stays in the approver's Received list — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Send a report for approval. 2. As the approver, reject it. 3. Look at Received.
  - Expected: Product decision: should a rejected report leave Received?
  - Actual: It stays in Received.

- **PR-04** Sending one report changes another report's Updated time — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Have two drafts, R1 and R2. 2. Send only R2. 3. Look at R1's Updated time.
  - Expected: R1's Updated time does not change.
  - Actual: R1's Updated time changed.

- **PR-05** Tools tile still says "Production Report Creation" after the member's posting right is removed — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Admin A turns member B's Production Report Post right OFF. 2. On B, look at the Tools tile and open the tool.
  - Expected: The tile and the module show the same name for a member without posting rights.
  - Actual: The tile still reads "Production Report Creation" while the module itself is titled "Production Report".

- **PR-06** Report History rows for Created, Updated and Sent for Signature show no role — Low · Web: Not checked, Android: Not checked
  - Steps: 1. A creates, updates and sends a report for signature; B approves or rejects it. 2. Open the report's History.
  - Expected: Every History row shows the person's role.
  - Actual: B's rows (Approved, Rejected) and A's Reminder Sent show a role; A's Created, Updated and Sent for Signature rows don't.
