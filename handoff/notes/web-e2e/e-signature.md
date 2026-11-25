# Web E2E — E-Signature

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: written — Skips: the tool is not enabled in the sandbox project

## Web test cases (15)

### E-Signature

- **module loads with the manager mode switch @smoke** — skipped (esignature.spec.js:66)
  - Where: Tools → Z-E-Signature · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Check that the mode switch at the top is shown → Check that it offers “Upload Document” and “Sign Documents”
  - Expected: Z-E-Signature opens the admin view with its mode switch, including Upload Document and Sign Documents.

- **Upload Document segment shows the envelope tabs and toolbar @smoke** — skipped (esignature.spec.js:76)
  - Where: Tools → Z-E-Signature → Upload Document · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Check that the Z-E-Signature, Completed and Rejected tabs are shown → Check that the Draft and Sent tabs are shown under it → Check that the “Upload Doc for Z-E-Signature” button is shown → Check that the “Search envelopes...” box is shown
  - Expected: The Upload Document view shows the envelope tabs (Z-E-Signature, Completed, Rejected, Draft, Sent), the upload button and the envelope search.

- **Sent tab renders its envelope panel @smoke** — skipped (esignature.spec.js:96)
  - Where: Tools → Z-E-Signature → Upload Document → Sent tab · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click the Sent tab → Check that Sent becomes the selected tab → Check that the Sent tab’s panel is shown
  - Expected: The Sent tab can be selected and shows its envelope panel (with or without envelopes).

- **Sign Documents segment shows the receiver tabs @smoke** — skipped (esignature.spec.js:108)
  - Where: Tools → Z-E-Signature → Sign Documents · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “Sign Documents” in the mode switch → Check that the Action Required tab is shown → Check that the Completed and Rejected tabs are shown
  - Expected: The Sign Documents view shows the signer’s tabs: Action Required, Completed and Rejected.

- **Templates segment opens @smoke** — skipped (esignature.spec.js:123)
  - Where: Tools → Z-E-Signature → Templates · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “Templates” in the mode switch → Check that the “Search templates” box is shown
  - Expected: The Templates view opens with its template search box.

- **Bulk Sends segment opens @smoke** — skipped (esignature.spec.js:131)
  - Where: Tools → Z-E-Signature → Bulk Sends · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “Bulk Sends” in the mode switch → Check that the Refresh button is shown
  - Expected: The Bulk Sends view opens with its Refresh button.

- **My Saved Signatures picker opens @smoke** — skipped (esignature.spec.js:139)
  - Where: Tools → Z-E-Signature · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “My Saved Signatures” → Check that a dialog or side panel about signatures opens
  - Expected: My Saved Signatures opens the saved-signature picker.

- **envelope list/card view toggle works @smoke** — skipped (esignature.spec.js:150)
  - Where: Tools → Z-E-Signature → Upload Document · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click the Card view button → Click the List view button → Check that the “Search envelopes...” box is still shown
  - Expected: Switching the envelope list to card view and back to list view keeps the toolbar in place. (The test does not check how the envelopes are drawn.)

- **envelope search accepts a query @smoke** — skipped (esignature.spec.js:158)
  - Where: Tools → Z-E-Signature → Upload Document · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Type “zzz-no-match-e2e” in “Search envelopes...” → Check that the box keeps the typed text
  - Expected: The envelope search box accepts a query. (The test does not check the filtered results.)

- **upload a document opens the editor Prepare step** — skipped (esignature.spec.js:196)
  - Where: Tools → Z-E-Signature → Upload Document · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “Upload Doc for Z-E-Signature” and pick the PDF sample.pdf → Wait until the hint reads “Add at least one signer to continue” (the upload has finished) → Check that the file name sample.pdf is shown → Check that a Replace button is shown
  - Expected: Uploading a PDF opens the envelope editor’s Prepare step showing the file name and a Replace button. The PDF is uploaded, but no draft is saved and nothing is sent.

- **Add Signer drawer opens** — skipped (esignature.spec.js:202)
  - Where: Tools → Z-E-Signature → Upload Document · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “Upload Doc for Z-E-Signature” and pick the PDF sample.pdf → Wait until the hint reads “Add at least one signer to continue” (the upload has finished) → Click Add Signer → Check that the “Add Signers” side panel opens
  - Expected: In the Prepare step, Add Signer opens the Add Signers panel. The PDF is uploaded, but no draft is saved and nothing is sent.

- **Add CC drawer opens** — skipped (esignature.spec.js:210)
  - Where: Tools → Z-E-Signature → Upload Document · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “Upload Doc for Z-E-Signature” and pick the PDF sample.pdf → Wait until the hint reads “Add at least one signer to continue” (the upload has finished) → Click Add CC → Check that the “Add CC Recipients” side panel opens
  - Expected: In the Prepare step, Add CC opens the Add CC Recipients panel. The PDF is uploaded, but no draft is saved and nothing is sent.

- **"I need to sign this document" adds self as a signer** — skipped (esignature.spec.js:218)
  - Where: Tools → Z-E-Signature → Upload Document · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “Upload Doc for Z-E-Signature” and pick the PDF sample.pdf → Wait until the hint reads “Add at least one signer to continue” (the upload has finished) → Click “I need to sign this document” → Check that the signer list reads “Signers (1)”
  - Expected: Ticking “I need to sign this document” adds you as the one signer. The PDF is uploaded, but no draft is saved and nothing is sent.

- **Add message accepts a subject and body** — skipped (esignature.spec.js:228)
  - Where: Tools → Z-E-Signature → Upload Document · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “Upload Doc for Z-E-Signature” and pick the PDF sample.pdf → Wait until the hint reads “Add at least one signer to continue” (the upload has finished) → If the subject field is hidden, click “Add message” to expand it → Type “[e2e] subject <time>” in “Enter subject” → Type “e2e message body” in the message field → Check that the subject field keeps the typed subject
  - Expected: The envelope’s Add message section accepts a subject and a message. The PDF is uploaded, but no draft is saved and nothing is sent.

- **advancing to Place fields shows the field toolbar** — skipped (esignature.spec.js:241)
  - Where: Tools → Z-E-Signature → Upload Document · account: iPhone XRR
  - Steps: Open Tools → Z-E-Signature (close the welcome tour if it appears) → Click “Upload Doc for Z-E-Signature” and pick the PDF sample.pdf → Wait until the hint reads “Add at least one signer to continue” (the upload has finished) → Click “I need to sign this document” → Check that “Next: Place fields” becomes enabled and click it → Check that the Signature field tile (“Drag or click to place Signature”) is shown → Check that a Save Draft button is shown
  - Expected: With a signer added, Next: Place fields opens the placement step with the field tiles and Save Draft. The PDF is uploaded, but no draft is saved and nothing is sent.

## Sheet bugs (2)

- **ESIGN-01** Reopening an envelope in the same session never shows the document — High · Web: Not checked, Android: Not checked
  - Steps: 1. Open an envelope to sign and go back. 2. Open the same envelope again.
  - Expected: The PDF renders and can be signed.
  - Actual: The document spinner never finished, so the envelope could not be signed.

- **ESIGN-02** Use Template does not load the template's document — High · Web: Not checked, Android: Not checked
  - Steps: 1. Save a template with a document. 2. Tap Use Template on it (twice, in one session).
  - Expected: The document loads both times.
  - Actual: The document did not load.
