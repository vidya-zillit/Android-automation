# Call Sheet (Home unit): web E2E vs Android Appium gap analysis

Web source: `web-e2e/call-sheet.md` (9 tests, 3 sheet bugs). It covers the Home → Call Sheet unit only.
Android suites:
- `tests/callsheet/CallSheetTest.java` (5 cases)
- `tests/callsheet/CallSheetUploadModeTest.java` (17 cases)
- for the sheet bugs, `tests/callsheetcreation/CallSheetCreationTest.java` and `CallSheetApprovalTest.java` (untracked, new)

App source: `java/com/zillit/zillitapp/bottomNav/home/Home.kt`, `baseUtils/CommonKeyboardFragment.kt`.

## Summary

| Class | Web tests |
|---|---|
| COVERED | 4 (2 weak) |
| MISSING | 5 (one proposed case group) |
| WEB-ONLY | 0 |
| CONFLICT | 0 |
| **Total** | **9** |

Sheet bugs:
- CS-01 and CS-02 are COVERED by the new Call Sheet Creation suite.
- CS-03 is MISSING.

Key fact: **on Android the Call Sheet unit accepts typed text.**
- The composer is registered for every unit (`Home.kt:405-408`). For Call Sheet, only the mic is hidden (`hideRecordingBtn`, Home.kt:413), and the attachment picker is limited to documents (Home.kt:966-983).
- But `AllUnitsMediaTest` treats Call Sheet as "document-only (no text/image/audio)" (`AllUnitsMediaTest.java:30`, :213-218) and skips text there. No Android test has ever posted, edited, replied to or deleted a text message on Call Sheet.
- This is not a CONFLICT, because it is an assumption rather than an assertion. Correct that comment when CS-M1 is added.

---

## MISSING

### CS-M1. Text messages on the Call Sheet unit: post, options, edit, reply, emoji (web #2, #3, #4, #5, #7)

The code is shared with Bulletin, where `HomeChatTest` and `ChatOptionsTest` cover it. These cases make sure the Call Sheet unit's own rules do not break it.

Steps (one chained class, or add to `CallSheetTest` after `historyButtonPresent`):
1. **Post.** Call Sheet chip in `recTab`. Type `[qa] cs-post <ts>` in `etMessage` and tap Send.
   - Expected: the row appears, and `etMessage` is empty.
2. **Options menu.** Long-press that text row.
   - Expected:
     - Reply, Edit and Delete are present.
     - Forward and Share are **absent** (`showForward` / `showShare` false on Call Sheet, Home.kt:1389-1391).
     - "Publish to Doc Distribution" is **absent** for text (`isShowDistributeToDD` needs a non-text type, Home.kt:1407).
   - The last point is the opposite of the web test's title. The web test never checked it. Close the menu.
3. **Edit.** Choose Edit, append " EDITED" and Update.
   - Expected: the row shows the new text, marked edited.
4. **Reply.** Choose Reply on the edited row. Check that the reply bar `llReply` shows, and that `ivAttachment` and `ivRecord` are hidden while replying (CommonKeyboardFragment.kt:384-392). Type `[qa] cs-reply <ts>` and Send.
   - Expected: the reply row quotes the original.
5. **Emoji.** Send `[qa] cs-emoji <ts> 🎬🔥✨👍` typed directly. Appium `sendKeys` handles these through UiAutomator2's Unicode IME. Fall back to the emoji keyboard if needed.
   - Expected: the row text contains all four emoji.
6. **Delete.** Long-press → Delete → action-bar delete → Yes on the row from step 1.
   - Expected: the row is gone after 3 s. Today `CallSheetTest.deleteDocument` never asserts this.
7. **Mic hidden.** With an empty composer on Call Sheet, `ivRecord` is not shown.
   - The Home gap list has this as M23 (`gaps/home.md`), so do not duplicate it.

---

## CONFLICT

None. See the note above about `AllUnitsMediaTest`'s "no text on Call Sheet" assumption.

---

## COVERED (web # → Android)

- #1 Call Sheet tab shows the composer and History → `CallSheetTest` "Call-sheet unit shows the History button" (`userHistoryBtn`), plus "READ: History button opens the call-sheet history". **Weak:** `etMessage` is never asserted on Call Sheet. Add `assertTrue(p.isInputBarVisible())`.
- #6 Delete a posted message → `CallSheetTest` "DELETE: remove a call-sheet document". **Weak:** it deletes a document, not text, and only checks that the Yes dialog appeared, not that the row disappeared. See CS-M1 step 6.
- #8 Upload a document in Continuation mode → `CallSheetUploadModeTest`:
  - "Sheet opens with nothing selected: Send disabled, picker hidden"
  - "Continuation enables Send and leaves the picker hidden"
  - "Continuation posts the document alongside the existing messages"
  - Also `CallSheetTest` "CREATE: send a document…"
- #9 Upload offers New, and Cancel uploads nothing → `CallSheetUploadModeTest` "New enables Send and leaves the picker hidden", "Cancel drops the picked files — nothing is uploaded" (plus "New asks a second time; 'No' aborts and posts nothing")

## WEB-ONLY

None.

## Sheet bugs vs Android

- **CS-01** The Save As sheet has the wrong title → COVERED by `CallSheetCreationTest` CSC-18 ("The name dialog is \"Save As New Draft\"…") and CSC-24 ("[QA CS-01] Save As makes a copy…").
- **CS-02** The version stays at v1 after saving again → COVERED by `CallSheetCreationTest` CSC-25 ("[QA CS-02] Saving again records an \"Updated\" revision v2 in History").
- **CS-03** No comment badge for the other user → **MISSING**. It needs two devices, like the two-device pattern in `TwoUserChatTest`.
  - Steps: device A sends a call sheet for comments to B (CSC-40/43 flow) and adds a comment.
  - Expected: on device B, an unread badge shows on the Call Sheet tool row on the Tools tab, and on the card in the Call Sheet list.
  - Android listens for this on the `isCallSheetCommentCreated` socket flow (`baseUtils/BaseSocketListener.kt:875-876`, emitted at :9221).
  - `CallSheetApprovalTest` CSC-60 already checks the badge on the Approvals card, so its badge locator can be reused.
