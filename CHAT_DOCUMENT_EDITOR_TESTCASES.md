# Chat document → app editor (view mode) — test cases

Suite: `tests/homechat/ChatDocumentEditorTest` (`TestOrder.CHAT_DOCUMENT_EDITOR` = 177)
Page: `pages/common/ChatDocumentEditorPage`; bubble tap: `HomeChatPage.tapNewestDocBubble()` / `openNewestDoc()`
Run: `./run-and-report.sh --only ChatDocumentEditorTest` (Android Studio: "Zillit ▸ Chat document → editor (view)")
Project: `ZL-Android-Automation` (the suite switches to it; override `-Dzillit.chatDocProject=`)

## What the app does (Zillit-Android `origin/development`)

`ChatAttachmentEditor.openChatAttachment` (f824d9211, 504942bef): an Office file
(docx/doc/odt/rtf, xlsx/xlsm/xls/ods/csv, pptx/ppt/odp) in a sent chat message (24-hex `_id`) on a
non-Box project asks Drive for `/editor/chat/<id>/config` (C&C) or
`/editor/unit-chat/<unit>/<id>/config` (unit chats), then opens `OnlyOfficeEditorActivity` with
`mode=view`. Any failure falls back silently to another app (`ACTION_VIEW`). PDF/txt/md always go to
another app. Repeat taps within 1.5 s are ignored.

## Checks per positive case

| # | Check | How (calibrated on the Pixel, 2026-11-23) |
|---|---|---|
| 1 | The editor opens, not another app | `currentActivity` contains `OnlyOfficeEditorActivity` within 30 s |
| 2 | The page loads | WebView present, "Loading editor…" gone, no "Failed to load editor" (60 s) |
| 3 | Read-only | page `status-text` (chat file) or `notice` (Drive Open) reads "View only" (edit mode reads "Live"); the app's native `Button` "Edit" (outside the WebView) absent; no editing tools |
| 4 | Back | ONE Back → same chat screen within 8 s; no "Saving…" (edit-mode close), no leave/notify dialog |
| 5 | Cleanup | the sent message (caption `ZL_AUTO chatdoc …`) is deleted |

Calibration notes (Zillit_32539-dev, origin/development cc3dc2a96): a chat file exposes `chrome`, `strip`,
`status-text` "View only", `stage`, `editor`, `zillit_docs_canvas` and no Button; a Drive "Open" adds `people`,
`notice` "View only." and the native "Edit" Button; edit mode shows `status-text` "Live" and no notice.
No toolbar nodes in either mode (canvas). Back from edit mode shows "Saving…" first.
Unit-chat doc bubbles render in `landScapeView` (not `centerView`) inside `imageParent`, with the
`attachmentType` badge bottom-left; C&C doc cards are `srcImage` with a `mimeTypeImage` badge.

## Cases and results

Pixel 7 (Vidya Pixel), Zillit_32539-dev, runs 1–2 on 2026-11-23.

| Case | Chat | Fixture | Result |
|---|---|---|---|
| CDE-01 | Bulletin | docx | PASS (editor, "View only", no Edit button, one Back) |
| CDE-02 | Bulletin — negative control | pdf | SKIP both runs: system picker did not reach `zl_test_doc.pdf` (helper issue); now falls back to the Download copy `zl_drive_doc.pdf` — not yet re-run |
| CDE-03 | Call Sheet | xlsx | PASS |
| CDE-04 | C&C 1:1 | docx | **FAIL — app bug** (see below) |
| CDE-05 | C&C group | xlsx | SKIP: no group for this account; suite now creates "QA Cc Grp <n>" — not yet re-run |
| CDE-06 | Info | docx | PASS run 1; run 2 suite timing bug (old photo satisfied "sent") — fixed, not re-run |
| CDE-07 | Confidential Info | xlsx | PASS run 1; same timing bug run 2 — fixed, not re-run |
| CDE-08 | Catering | docx | SKIP: "You cannot post any message until a person joins the catering department" |
| CDE-09 | Production Report chat (tool `production_report_tool_chat_label`) | docx | PASS |
| CDE-10 | Accounts | docx | SKIP: "…until a person joins the accounts department" |
| CDE-11 | Casting (Main Cast) → chat | — | SKIP: Selects chat (OtherChatsActivity) has no attachment button |
| CDE-12 | Costume (Main Cast) → chat | — | SKIP: same, no attachment button |
| CDE-13 | Location | — | SKIP: Location tool deferred by design |
| CDE-14 | Documents & Signature → Standard Documents → Chat with Users (FormDiscussionChatPage) | docx | PASS |
| CDE-15 | [expected-fail] FormChatPage (form/contract chat window) | — | SKIP: needs a sent form with a signer discussion (would send a real signature request); source still calls `openFileFromExternalFromUrl` |

## App bugs found

1. **C&C documents never open in the editor** (Drive #202 path). A just-sent C&C docx makes no
   `/editor/chat/<id>/config` call at all and opens Google Docs (the fresh row has no stored 24-hex `_id`).
   After leaving and re-entering the thread, the call is made and Drive answers
   **`400 chat_attachment_storage_unsupported`** for the C&C attachment (`media=chat/file/<project>/Zillit_….docx`),
   so it still falls back to another app. Unit chats on the same project work.
2. The Production Report chat tool's label is shown as the raw key **`production_report_tool_chat_label`**
   (Tools list and chat header) — missing string.
3. (Source) `FormChatPage` lost its editor wiring in merge 59332b63b — not reproduced on device.

Calibration side note: unit-chat doc bubbles render in `landScapeView` (not `centerView`).
