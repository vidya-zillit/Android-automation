# Document Distribution — test cases (web E2E gap cases)

The Android suites are in `src/test/java/com/zillit/automation/tests/docdist/`.

This file covers the cases added on 2026-11-23 to close the gap with the web E2E report of 2026-11-17. The gap analysis is `web-e2e/gaps/document-distribution.md` in the side-session notes. Its rows are named M1 to M34, plus C1.

Each case has its web id, the sheet bug it guards (DD-xx) and its last result on device. The installed dev build (Zillit_32539-dev) already carries the fixes for most DD bugs. On that build the cases are expected to pass. On a build without a fix, a case fails and names the bug.

Runs: emulator-5554 / emulator-5556 (user Vidya Testing), `ZILLIT_FLAVOR=dev ./run-and-report.sh --only <Class>`.

## New suite: `DocDistWebGapTest` (`TestOrder.DOCDIST_WEB_GAP` = 195)

The suite seeds its own folders, each with a run stamp, `QA_ZL_AUTO_<ts>`:

- `_A` holds the PDF, the DOCX and the subfolder `_SUB`.
- `_B` has a description.
- `_X` is a plain folder.

C-12 bulk-deletes all of them, and CLEANUP is the safety net.

| Case | Gap row / web id | Bug | What it proves | Last result |
|---|---|---|---|---|
| SEED | — | — | creates `_A`, `_B`, `_X`; uploads PDF + DOCX into `_A`; creates `_A/_SUB` | PASS |
| MOVE-1 | M1 · L77 | DD-23 | "Move here" is on screen when the sheet opens and after the folder list is scrolled to its end. It adds 16 subfolders of `_X` when the project has too few folders to overflow the sheet. | run 1: SKIP (only 5 Move rows, so the list could not overflow; "Move here" was on screen). Now seeds 16 subfolders — rerun pending after a test fix |
| NEST-9 | M19 · L125 (Move half) | DD-30 | the Move list names a nested folder `_A / _SUB`, never by its bare name | PASS |
| D-15 | M32(b) · D-lifecycle | — | Watermark on a .docx row is refused (disabled, or the sheet shows "not supported" with Download off); enabled for the PDF | run 1: FAIL from a test artefact (a11y enabled flag read on the title). Now checks behaviour — rerun pending after a test fix |
| EXTRA-2 | M9 · L27 | DD-05 | a no-match search inside a folder keeps Publish/Distribute; no "Upload files to enable distribution" | PASS |
| B-14 | M8 · L33 | DD-06 (guard) | searching `pdf ` (trailing space) still lists the PDF | PASS |
| NEST-8 | M8 · L64 | DD-18 | a root search finds the nested folder and the nested docx, each meta line led by `_A` | PASS |
| C-4b | M30 · F02 | — | the folder row's meta line shows its description | PASS |
| C-11 | M15 · L39 | DD-07 | Edit folder: Save stays disabled for an empty and a spaces-only name; the name is kept | PASS |
| SORT-1 | M33 · D13 | — | Name (Z–A) and Name (A–Z) really reorder the rows | run 1 PASS; run 2 FAIL (test read off-screen rows). Now asserts the order of the rendered rows — rerun pending after a test fix |
| A-7 | M17 · L89 | DD-29 | a collapsed help card stays collapsed in a folder and back at the root | PASS |
| A11Y-1 | M18 · L94/L99 | DD-34 | content-desc: "List view", "Grid view", home crumb "Library home", row checkbox "Select <name>" (not "Select all") | PASS |
| SEL-SEARCH | M2 · L45 | DD-11 | tick PDF + DOCX, search `docx` → "1 selected"; Delete removes only the docx, the hidden PDF survives | PASS |
| MOVE-2 | M3 · D08, L118 | DD-17 / DD-14 | the PDF really moves `_A` → `_B`: one snackbar "Moved 1 item", gone from `_A`, listed in `_B` | run 1: FAIL from a test bug (a downward swipe dismissed the Move sheet) — rerun pending after a test fix |
| DL-1 | M32(a) · D-lifecycle | — | row menu Download → "<file> saved to Downloads"; the downloaded copy is then removed | not run yet (queued) |
| D-14 | M16 · L52 | DD-14 (guard) | a one-item date group reads "… · 1 item" | not run yet (queued) |
| MOVE-3 | M3 · F06 | — | folder `_X` moves into `_B`: its search result path names `_B`, it is gone from the root, listed inside `_B` | FAIL from test bugs (sheet swipe; list read before it loaded), both fixed — rerun pending after a test fix |
| C-12 | M31 · F09 | — | tick `_A` + `_B` → Delete → both gone, with `_SUB` and `_X` inside them | PASS (run 2) |
| LIVE-1 | M27 · F11 | — | two devices: a folder created and then deleted on device B appears and disappears on device A without a refresh | not run yet (queued two-device run) |
| CLEANUP | — | — | deletes anything this run left | PASS |

## Cases added to existing suites

| Suite | Case | Gap row / web id | Bug | Last result |
|---|---|---|---|---|
| DocDistComposerTest | F-27: Preview's To lists only the valid addresses | M6 · L173 | DD-26 | PASS |
| DocDistComposerTest | F-28: a folder row's Send opens "Distribute folder" with its files | M7 · L178/F10 | DD-28 | PASS |
| DocDistComposerTest | F-29: Compose and Share documents inside a folder are "Compose email" | M7 · L178 | DD-28 | PASS |
| DocDistComposerTest | F-30: Attach from library leads each row with its folder | M19 · L125 (Attach half) | DD-30 | PASS |
| DocDistTemplatesTest | H-11: a template over a typed subject asks first; Keep keeps, Replace loads | M5 · L191/C06 | DD-20 | PASS |
| DocDistTemplatesTest | H-12: a loaded template keeps the signature | M5 · L198 | DD-20 | SKIP: the composer WebView exposes no text, so the signature cannot be read |
| DocDistTemplatesTest | H-13: a second template with the same name is refused | M11 · L235 | DD-31 | run 2: refusal verified (error shown, editor stayed open); the read-back of the manager was a test bug, now fixed — rerun pending after a test fix |
| DocDistPresetsTest | G-11: leaving a list with unsaved members asks first; Discard creates nothing | M10 · L217 | DD-16 | PASS |
| DocDistPresetsTest | G-12: a second list with the same name is refused | M11 · L223 | DD-19 | PASS |
| DocDistPresetsTest | G-13: a one-member list reads singular ("1 recipient") | M16 · L229 | DD-14 | FAIL from a wrong expectation (the app reads "1 recipient", which is singular and correct). Fixed — rerun pending after a test fix |
| DocDistPresetsTest | G-15: CSV import adds the 2 good rows of 4 (invalid + repeat flagged), list saved, then deleted | M25 · L343 | — | FAIL from a test bug (pushFile needs base64) — rerun pending after a test fix |
| DocDistPresetsTest | G-14: a list row's Export CSV saves the members (file read back) | M24 · L337 | — | not run yet (queued) |
| DocDistPublishTest | N-15: one file reads "1 document"; heading "Publish in one of the modules below" | M16 · L52/L143 | DD-14 | PASS |
| DocDistWatermarkTest | M-13: the download sheet's note says "this download only" | M16 · L143 | DD-22 | SKIP at an unlogged step; diagnostics added — rerun pending after a test fix |
| DocDistWatermarkTest | M-14: a ticked .docx is left off the batch and named as skipped | M20 · L150 | DD-33 | not run yet (queued) |
| DocDistHistoryTest | J-3 (strengthened): a no-match search says "No matching emails.", and clearing restores the plain text | M28 · L410 | DD-27 | PASS on an empty history; now also checks the no-match text on an empty history — rerun pending after a test fix |
| DocDistHistoryTest | K-8b: Save as list creates the list under Lists (then deleted) | M34 · H08 | — | SKIP: this project has no History records. The queued run sends one first (DocDistSendTest, TestEmails only) |
| DocDistContactsTest | I-13b: the exported address-book CSV starts with a UTF-8 BOM (file read back) | M23 · L248 | DD-09 | not run yet (queued) |

## Fixes to existing cases made along the way

- **Composer leave with drafts.** The composer now asks "Keep this email as a draft?" when leaving with content. `leaveComposer`, `leaveComposerDiscarding` (ComposerTest), H-9 (TemplatesTest) and G-10 (PresetsTest) now answer **Discard**. Before, they accepted "Keep draft", which left QA drafts behind. F-26's assertions are unchanged.
- **Attachment rows below the fold.** `DocDistComposerPage.attachmentNames()/attachmentCount()` now scroll the attachment list into view, because the rows were off-screen and read as "no attachments". E-7 had failed on this.
- **Unsaved-changes prompt on the preset editor.** `DocDistPresetsPage.forceCloseEditor()` answers DD-16's prompt with Discard.
- **G-10 multi-choice list picker.** The composer's list picker is now multi-choice. G-10 taps Done before it reads the chips.

## Not automatable (with reason)

| Gap row / web id | Why |
|---|---|
| M4 O-7/O-8 + R99 (lowered rights: no Post / no View) | This needs an admin to turn a real second user's Document Distribution rights off, then back on. Only the Pixel admin can do that, and it is often locked (the PIN is off-limits). A failed restore would leave a real user without rights. That counts as a rights change with no safe restore. |
| M12 K-11 (History detail that fails to load) | This is an offline / network-fault case. Cutting connectivity on a shared device breaks other agents' runs, per the brief. |
| M13 P-10 / P-11 / P-12 (offline Create folder / Move / Watermark download) | Same as K-11: offline cases on shared devices. |
| M14 I-15 (Contacts that fail to load, then Retry) | Same as K-11: offline case. |
| M21 H-14 (stored template script never runs) | It needs a template whose body holds raw HTML (`<img onerror>`). The Android template editor is a RichEditor WebView that escapes typed text, and the harness has no API client to seed one. So it needs a fixture we cannot create. |
| M5 H-10 (a new compose starts with the saved signature) | The composer body is a JavaScript WebView that exposes no text to the accessibility tree on these devices. The signature cannot be read, and H-12 skips for the same reason. |
| M22 (signature with a name and no body is refused) | A save the server accepts calls `createSignature(name, content, useForNew = true, useForReply = true)`. That makes the empty signature the account's default for new and reply emails. The previous default cannot be restored from the test. This is an account setting change with no safe restore, and the screen belongs to the Email module. |
| M26 P03 (Publish to Doc Distribution from Info) | It posts a document into the Info module of the shared real project, which every member sees. This is the same boundary as N-9. |
| M29 P02 (Publish to Info actually posts) | N-9 stays `@Disabled` for the same reason: it is a cross-module write into a shared project, and the web test stubbed it. |
| M27 H11 (History refreshes on a teammate's send) | Each run would send a real email from the second device. The folder half of M27 (LIVE-1) covers the same socket path. |
